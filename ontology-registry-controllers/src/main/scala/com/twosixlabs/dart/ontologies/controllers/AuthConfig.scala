package com.twosixlabs.dart.ontologies.controllers

import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies

case class AuthConfig( override val secretKey : Option[ String ],
					   override val useDartAuth : Boolean,
					   override val basicAuthCredentials : Seq[ (String, String) ] = Nil )
  extends AuthDependencies
