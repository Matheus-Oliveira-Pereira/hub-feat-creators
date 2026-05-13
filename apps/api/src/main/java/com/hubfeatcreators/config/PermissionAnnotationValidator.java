package com.hubfeatcreators.config;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/**
 * Falha boot se algum {@link RequirePermission} usar código fora de {@link PermissionCodes#ALL}.
 * Previne código morto silencioso (ex: "INFL_R" que ninguém possui).
 */
@Component
public class PermissionAnnotationValidator {

    private static final Logger log = LoggerFactory.getLogger(PermissionAnnotationValidator.class);

    private final ApplicationContext ctx;

    public PermissionAnnotationValidator(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        List<String> invalid = new ArrayList<>();
        for (String name : ctx.getBeanNamesForAnnotation(RestController.class)) {
            Object bean = ctx.getBean(name);
            Class<?> clazz = AopUtils.getTargetClass(bean);
            for (Method m : clazz.getDeclaredMethods()) {
                RequirePermission ann = m.getAnnotation(RequirePermission.class);
                if (ann == null) continue;
                for (String code : ann.value()) {
                    if (!PermissionCodes.ALL.contains(code)) {
                        invalid.add(
                                clazz.getSimpleName() + "." + m.getName() + " → \"" + code + "\"");
                    }
                }
            }
        }
        if (!invalid.isEmpty()) {
            log.error(
                    "permission.validator FAIL: códigos fora de PermissionCodes.ALL: {}", invalid);
            throw new IllegalStateException("@RequirePermission usa códigos inválidos: " + invalid);
        }
        log.info("permission.validator ok");
    }
}
