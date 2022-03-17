package com.twosixlabs.dart.ontologies.api

import com.twosixlabs.dart.rest.ApiStandards
import com.twosixlabs.dart.rest.scalatra.models.FailureResponse
import io.circe.generic.auto._
import sttp.model.{HeaderNames, Method, StatusCode}
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.server.interceptor.decodefailure.DecodeFailureHandler
import sttp.tapir.server.interceptor.{DecodeFailureContext, ValuedEndpointOutput}

import scala.collection.mutable.ListBuffer


trait DartServiceApiDefinition {


    /**
     * Name of the service
     */
    val serviceName : String

    /**
     * Slug to be used as base path of the service if different from servicename
     */
    val servicePathName : Option[ String ]

    final lazy val basePath : String = ApiStandards.DART_API_PREFIX_V1 + "/" + servicePathName.getOrElse( serviceName.toLowerCase )
    private lazy val basePathSections = basePath.stripPrefix( "/" ).split( '/' ).map( _.trim )

    /**
     * Registry of endpoints for this service, to be initialized by calling addToDart
     * on a tapir endpoint
     */
    private val endpointList : ListBuffer[ Endpoint[ _, _, _, _ ] ] = ListBuffer[ Endpoint[ _, _, _, _ ] ]()

    def notFoundErr( desc : String ) : EndpointOutput.OneOfMapping[ FailureResponse ] = oneOfMapping( StatusCode.NotFound, jsonBody[ FailureResponse ].description( desc ) )

    def badRequestErr( desc : String ) : EndpointOutput.OneOfMapping[ FailureResponse ] = oneOfMapping( StatusCode.BadRequest, jsonBody[ FailureResponse ].description( desc ) )

    def serviceUnavailableErr( desc : String ) : EndpointOutput.OneOfMapping[ FailureResponse ] = oneOfMapping( StatusCode.ServiceUnavailable, jsonBody[ FailureResponse ].description( desc ) )

    def authenticationFailure( desc : String ) : EndpointOutput.OneOfMapping[ FailureResponse ] = oneOfMapping( StatusCode.Unauthorized, jsonBody[ FailureResponse ].description( desc ) )

    def authorizationFailure( desc : String ) : EndpointOutput.OneOfMapping[ FailureResponse ] = oneOfMapping( StatusCode.Forbidden, jsonBody[ FailureResponse ].description( desc ) )

    /**
     * Register a defined endpoint, adding to it any possible additional mapped responses to
     *
     * @param endpt         endpoint to be added to registry
     * @param errorMappings status mappings to be added to endpoint
     * @return
     */
    private def AddToDart[ I, E, O, R ]( endpt : Endpoint[ I, E, O, R ], errorMappings : EndpointOutput.OneOfMapping[ FailureResponse ]* ) : Endpoint[ (String, I), (FailureResponse, E), O, R ] = {
        val dartEndPt = endpt
          .tag( serviceName )
          .prependErrorOut(
              oneOf[ FailureResponse ](
                  authenticationFailure( "Authentication token missing or invalid" ),
                  authorizationFailure( "User not authorized for this operation" )
                  +: errorMappings : _*,
                  )
              )
          .prependIn( auth.bearer[ String ]() )
          .prependIn( basePathSections.tail.foldLeft( basePathSections.head : EndpointInput[ Unit ] )( _ / _ ) / servicePathName.getOrElse( serviceName.toLowerCase ) )

        endpointList += dartEndPt
        dartEndPt
    }

    private def statusCodeOut( codeGetter : StatusCode.type => StatusCode ) : EndpointOutput.FixedStatusCode[ Unit ] = {
        EndpointOutput.FixedStatusCode( codeGetter( StatusCode ), Codec.idPlain(), EndpointIO.Info.empty )
    }

    private def valuedOutput( codeGetter : StatusCode.type => StatusCode, msg : String ) : Option[ ValuedEndpointOutput[ FailureResponse ] ] = {
        val sco = statusCodeOut( codeGetter )
        val scInt = sco.statusCode.code
        val failureResponse = FailureResponse( scInt, msg )
        Some( ValuedEndpointOutput[ FailureResponse ]( sco.and( jsonBody[ FailureResponse ] ), failureResponse ) )
    }

    def failureMessage( failure : DecodeResult.Failure ) : String = failure match {
        case DecodeResult.Missing => "missing value"
        case DecodeResult.Multiple( vs ) => s"""multiple values provided: ${vs.mkString( "; " )}"""
        case DecodeResult.Error( original, DecodeResult.Error.JsonDecodeException( errors, _ ) ) =>
            "errors encountered decoding JSON:" +
            errors.foldLeft( ("", 0) )( ( foldTup, error ) => {
                val (currentString, lastIndex) = foldTup
                val thisIndex = lastIndex + 1
                (currentString + s"""\n${thisIndex}: ${error.msg}${if ( error.path.nonEmpty ) " (field: '" + error.path.map( _.name ).mkString( "." ) + "')" else ""})""", thisIndex)
            } )._1 +
            s"""\n\nOriginal:\n${original}"""
        case DecodeResult.Error( original, error : Throwable ) =>
            s"""unknown decoding error: ${error.getMessage}\n\nOriginal:\n${original}"""
        case DecodeResult.Mismatch( expected, actual ) =>
            s"""expected ${expected}; received ${actual}"""
        case DecodeResult.InvalidValue( errors ) =>
            s"""invalid value(s):""" +
            errors.foldLeft( ("", 0) )( ( foldTup, error ) => {
                val (currentMessage, lastIndex) = foldTup
                val thisIndex = lastIndex + 1
                val errorMsg = error match {
                    case ValidationError.Custom( invalidValue, msg, path ) =>
                        s"""${invalidValue}${if ( path.exists( _.name.trim.nonEmpty ) ) s" at ${path.mkString( "." )}" else ""} - ${msg}"""
                    case ValidationError.Primitive( validator, invalidValue, path ) =>
                        val msg = validator match {
                            case Validator.Min( value, exclusive ) =>
                                s"must be greater than ${if ( exclusive ) "" else "or equal to "}${value}"
                            case Validator.Max( value, exclusive ) =>
                                s"must be less than ${if ( exclusive ) "" else "or equal to "}${value}"
                            case Validator.Pattern( value ) =>
                                s"must match ${value}"
                            case Validator.MinLength( value ) =>
                                s"must have length greater than or equal to $value"
                            case Validator.MaxLength( value ) =>
                                s"must have length greater than or equal to $value"
                            case Validator.MinSize( value ) =>
                                s"must have size greater than or equal to $value"
                            case Validator.MaxSize( value ) =>
                                s"must have size less than or equal to $value"
                            case Validator.Enumeration( possibleValues, encode, name ) =>
                                s"must be one of: ${
                                    possibleValues.map( v => encode match {
                                        case Some( encoder ) => encoder( v )
                                        case None => v
                                    } ).mkString( "; " )
                                }"
                        }
                        s"""${invalidValue}${if ( path.exists( _.name.trim.nonEmpty ) ) s" at ${path.mkString( "." )}" else ""} - ${msg}"""
                }
                (currentMessage + s"""\n${thisIndex}: ${errorMsg}${if ( error.path.nonEmpty ) s" (${error.path.mkString( "." )})" else ""}""", thisIndex)
            } )._1
    }

    private def failingInput( ctx : DecodeFailureContext ) = {
        import sttp.tapir.internal.RichEndpointInput
        //        ctx.failure match {
        //            case DecodeResult.Missing =>
        //                val missingAuth = ctx.endpoint.input.pathTo(ctx.failingInput).collectFirst { case a: EndpointInput.Auth[_] =>
        //                    a
        //                }
        //                missingAuth.getOrElse(ctx.failingInput)
        //            case _ => ctx.failingInput
        //        }
        val missingAuth = ctx.endpoint.input.pathTo( ctx.failingInput ).collectFirst { case a : EndpointInput.Auth[ _ ] =>
            a
        }
        missingAuth.getOrElse( ctx.failingInput )
    }

    val decodeFailureHandler = new DecodeFailureHandler {
        override def apply(
          ctx : DecodeFailureContext
        ) : Option[ ValuedEndpointOutput[ _ ] ] = {
            def msg( prefix : String ) = prefix + ": " + failureMessage( ctx.failure )

            failingInput( ctx ) match {
                case q : EndpointInput.Query[ _ ] =>
                    valuedOutput( _.BadRequest, msg( s"Bad query parameter: ${q.name}" ) )
                case q : EndpointInput.QueryParams[ _ ] =>
                    valuedOutput( _.BadRequest, msg( s"Badly formed query string}" ) )
                case h : EndpointIO.Header[ _ ] if ctx.failure.isInstanceOf[ DecodeResult.Mismatch ] && h.name == HeaderNames.ContentType =>
                    valuedOutput( _.UnsupportedMediaType, s"Unsupported media type" )
                case h : EndpointIO.Header[ _ ] => valuedOutput( _.BadRequest, msg( s"Invalid header : ${h.name}" ) )
                case fh : EndpointIO.FixedHeader[ _ ] if ctx.failure.isInstanceOf[ DecodeResult.Mismatch ] && fh.h.name == HeaderNames.ContentType =>
                    valuedOutput( _.UnsupportedMediaType, s"Unsupported media type: ${fh.h.value}" )
                case h : EndpointIO.FixedHeader[ _ ] => valuedOutput( _.BadRequest, msg( s"Invalid header: ${h.h.name}: ${h.h.name}" ) )
                case h : EndpointIO.Headers[ _ ] => valuedOutput( _.BadRequest, msg( s"Invalid headers" ) )
                case b : EndpointIO.Body[ _, _ ] => valuedOutput( _.BadRequest, msg( s"Bad request body" ) )
                case b : EndpointIO.StreamBodyWrapper[ _, _ ] => valuedOutput( _.BadRequest, msg( s"Bad request body" ) )

                // we assume that the only decode failure that might happen during path segment decoding is an error
                // a non-standard path decoder might return Missing/Multiple/Mismatch, but that would be indistinguishable from
                // a path shape mismatch
                case p : EndpointInput.PathCapture[ _ ] =>
                    valuedOutput( _.BadRequest, msg( s"Bad path parameter${p.name.map( v => s": $v" ).getOrElse( "" )}" ) )
                case a : EndpointInput.Auth[ _ ] =>
                    val potentialCause = ctx.failingInput match {
                        case h : EndpointIO.Header[ _ ] => s": ${h.name} header"
                        case fh : EndpointIO.FixedHeader[ _ ] => s": ${fh.h.name} header"
                        case _ => ""
                    }
                    valuedOutput( _ ( 403 ), msg( s"Authentication failure${potentialCause}" ) )
                // other basic endpoints - the request doesn't match, but not returning a response (trying other endpoints)
                case _ : EndpointInput.Basic[ _ ] => None
                // all other inputs (tuples, mapped) - responding with bad request
                case _ => valuedOutput( _.BadRequest, msg( "Bad request" ) )
            }
        }
    }

    protected implicit class RegisterableEndpoint[ I, E, O, -R ]( endpt : Endpoint[ I, E, O, R ] ) {
        def addToDart( errorMappings : EndpointOutput.OneOfMapping[ FailureResponse ]* ) : Endpoint[ (String, I), (FailureResponse, E), O, R ] = {
            AddToDart( endpt, errorMappings : _* )
        }
    }

    def allEndpoints : List[ Endpoint[ _, _, _, _ ] ] = endpointList.toList

    private val openApiOps : OpenAPIDocsOptions = OpenAPIDocsOptions(
        ( ids : Vector[ String ], method : Method ) => method.method.toLowerCase + ids.drop( basePathSections.length ).map( s => {
            val charArray = s.toLowerCase.toCharArray
            charArray( 0 ) = Character.toUpperCase( charArray( 0 ) )
            new String( charArray )
        } ).mkString
        )

    def openApiSpec : String = {
        OpenAPIDocsInterpreter( openApiOps )
          .toOpenAPI( allEndpoints, serviceName, "1.0" )
          .toYaml
    }

}
