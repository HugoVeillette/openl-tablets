package org.openl.rules.dt.element;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;

public final class ConditionHelper {
    private ConditionHelper() {
    }

    private static IOpenCast toNullIfNotImplicitCast(IOpenCast cast) {
        if (cast != null && cast.isImplicit()) {
            return cast;
        }
        return null;
    }

    public static final ConditionCasts findConditionCasts(IOpenClass conditionParameterType,
            IOpenClass inputType,
            IBindingContext bindingContext) {
        IOpenCast castToConditionType = toNullIfNotImplicitCast(
            bindingContext.getCast(inputType, conditionParameterType));
        IOpenCast castToInputType = castToConditionType == null ? toNullIfNotImplicitCast(
            bindingContext.getCast(conditionParameterType, inputType)) : null;
        return new ConditionCasts(castToInputType, castToConditionType);
    }

    public static ConditionCasts getConditionCastsWithNoCasts() {
        return CONDITION_CASTS_WITH_NO_CASTS;
    }

    private static final ConditionCasts CONDITION_CASTS_WITH_NO_CASTS = new ConditionCasts(null, null);
}
