package org.openl.types.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.base.INameSpacedThing;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;

public class OpenClassHelper {

    public static synchronized IOpenClass getOpenClass(IOpenClass moduleOpenClass, Class<?> classToFind) {
        IOpenClass result = null;
        if (classToFind != null) {
            Map<String, IOpenClass> internalTypes = moduleOpenClass.getTypes();
            if (classToFind.isArray()) {
                IOpenClass componentType = findType(classToFind.getComponentType(), internalTypes);
                if (componentType != null) {
                    result = componentType.getAggregateInfo().getIndexedAggregateType(componentType, 1);
                }
            } else {
                result = findType(classToFind, internalTypes);
            }

            if (result == null) {
                result = JavaOpenClass.getOpenClass(classToFind);
            }
        }
        return result;
    }

    private static IOpenClass findType(Class<?> classToFind, Map<String, IOpenClass> internalTypes) {
        IOpenClass result = null;
        for (IOpenClass datatypeClass : internalTypes.values()) {
            //getInstanceClass() for DomainOpenClass returns simple type == enum type
            if (!(datatypeClass instanceof DomainOpenClass) && classToFind.getName().equals(datatypeClass.getInstanceClass().getName())) {

                result = datatypeClass;
                break;
            }
        }
        return result;
    }

    public static synchronized IOpenClass[] getOpenClasses(IOpenClass moduleOpenClass, Class<?>[] classesToFind) {
        if (classesToFind.length == 0) {
            return IOpenClass.EMPTY;
        }

        List<IOpenClass> openClassList = new ArrayList<IOpenClass>();

        for (Class<?> classToFind : classesToFind) {
            openClassList.add(getOpenClass(moduleOpenClass, classToFind));
        }
        return openClassList.toArray(new IOpenClass[openClassList.size()]);

    }

}
