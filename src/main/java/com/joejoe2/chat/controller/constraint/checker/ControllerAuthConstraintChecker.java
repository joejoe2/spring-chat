package com.joejoe2.chat.controller.constraint.checker;

import com.joejoe2.chat.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.chat.exception.ControllerConstraintViolation;
import com.joejoe2.chat.utils.AuthUtil;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Component
public class ControllerAuthConstraintChecker {
    public void checkWithMethod(Method method) throws ControllerConstraintViolation {
        checkAuthenticatedApiConstraint(method);
    }

    private static void checkAuthenticatedApiConstraint(Method method) throws ControllerConstraintViolation {
        AuthenticatedApi constraint = method.getAnnotation(AuthenticatedApi.class);
        if (constraint != null) {
            if (!AuthUtil.isAuthenticated())
                throw new ControllerConstraintViolation(
                        constraint.rejectStatus(), constraint.rejectMessage());
        }

        for (Annotation annotation : method.getAnnotations()) {
            constraint = annotation.annotationType().getAnnotation(AuthenticatedApi.class);
            if (constraint != null) {
                if (!AuthUtil.isAuthenticated())
                    throw new ControllerConstraintViolation(constraint.rejectStatus(),
                            constraint.rejectMessage());
                else break;
            }
        }
    }
}
