package com.hubfeatcreators.infra.security.rbac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que o {@link com.hubfeatcreators.infra.security.AuthPrincipal} autenticado contenha
 * pelo menos uma (default {@link Mode#ANY_OF}) ou todas (modo {@link Mode#ALL_OF}) das
 * permissões listadas em {@link #value()}. OWNER coarse e role OWNR bypassam.
 *
 * <p>Aplicada em métodos de controller; aspect {@code RequirePermissionAspect} intercepta.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

  String[] value();

  Mode mode() default Mode.ANY_OF;

  enum Mode {
    ANY_OF,
    ALL_OF
  }
}
