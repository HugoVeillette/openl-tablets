/**
 * 
 */
package org.openl.rules.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.MethodSearch;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextDelegator;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenClassDelegator;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binding context for xls rules.
 * 
 * @author DLiauchuk
 *
 */
public class RulesModuleBindingContext extends ModuleBindingContext {
    private final Logger log = LoggerFactory.getLogger(RulesModuleBindingContext.class);

    public static String MODULE_PROPERTIES_KEY = "Properties:Module";
    public static String CATEGORY_PROPERTIES_KEY = "Properties:Category:";
    private Map<String, TableSyntaxNode> bindedTables = new HashMap<String, TableSyntaxNode>();

    /**
     * Internal OpenL service methods. 
     */
    private List<IOpenMethod> internalMethods;
    
    private PreBinderMethods preBinderMethods = new PreBinderMethods();
    
    public RulesModuleBindingContext(IBindingContext delegate,
            ModuleOpenClass module) {
        super(delegate, module);
        internalMethods = new ArrayList<IOpenMethod>();
        internalMethods.add(new CurrentRuntimeContextMethod());
        internalMethods.add(new EmptyRuntimeContextMethod());
        internalMethods.add(new RestoreRuntimeContextMethod());
        internalMethods.add(new SetRuntimeContextMethod());
        internalMethods.add(new ModifyRuntimeContextMethod());
    }
    
    /**
     * Registers the tsn by specified key.
     * 
     * @param key Key that have to be same for equivalent tables.
     * @param tsn TableSyntaxNode to register.
     */
    public void registerTableSyntaxNode(String key, TableSyntaxNode tsn) {
        this.bindedTables.put(key, tsn);
    }

    /**
     * @return <code>true</code> if key TableSyntaxNode with specified key has
     *         already been registered.
     */
    public boolean isTableSyntaxNodeExist(String key) {
        return this.bindedTables.containsKey(key);
    }

    public TableSyntaxNode getTableSyntaxNode(String key) {
        return bindedTables.get(key);
    }
    
    @Override
    public IMethodCaller findMethodCaller(String namespace, final String methodName, IOpenClass[] parTypes) throws AmbiguousMethodException {
        Iterable<IOpenMethod> select = CollectionUtils.findAll(preBinderMethods.values(),
            new CollectionUtils.Predicate<IOpenMethod>() {
                @Override
                public boolean evaluate(IOpenMethod method) {
                    return methodName.equals(method.getName());
                }
            });
        IMethodCaller method = MethodSearch.getCastingMethodCaller(methodName, parTypes, this, select);
        if (method != null) {
            RecursiveOpenMethodPreBinder openMethodBinder = null;
            if (method instanceof RecursiveOpenMethodPreBinder) {
                openMethodBinder = (RecursiveOpenMethodPreBinder) openMethodBinder;
            }
            if (method instanceof CastingMethodCaller) {
                openMethodBinder = (RecursiveOpenMethodPreBinder) ((CastingMethodCaller) method).getMethod();
            }
            method = null;
            if (openMethodBinder.isPreBinding()){
                method = super.findMethodCaller(namespace, methodName, parTypes);
                if (method == null) {
                    Iterable<IOpenMethod> internalselect = CollectionUtils.findAll(internalMethods, new CollectionUtils.Predicate<IOpenMethod>() {
                        @Override public boolean evaluate(IOpenMethod method) {
                            return methodName.equals(method.getName());
                        }
                    });
                    method = MethodSearch.getCastingMethodCaller(methodName, parTypes, this, internalselect);
                }
                if (method != null){
                    return method;
                }
                throw new RecursiveMethodPreBindingException();
            }
            openMethodBinder.preBind();
            preBinderMethods.remove(openMethodBinder.getHeader());
        }
        
        method = super.findMethodCaller(namespace, methodName, parTypes);
        if (method == null) {
            Iterable<IOpenMethod> internalselect = CollectionUtils.findAll(internalMethods, new CollectionUtils.Predicate<IOpenMethod>() {
                @Override public boolean evaluate(IOpenMethod method) {
                    return methodName.equals(method.getName());
                }
            });
            method = MethodSearch.getCastingMethodCaller(methodName, parTypes, this, internalselect);
        }
        return method;
    }
    
    @Override
    public IOpenClass findType(String namespace, String typeName) {
        if (OpenLSystemProperties.isCustomSpreadsheetType(getExternalParams())) {
            if (typeName.startsWith(Spreadsheet.CUSTOMSPREADSHEETRESULT_TYPE_PREFIX)){
                String sprMethodName = typeName.substring(Spreadsheet.CUSTOMSPREADSHEETRESULT_TYPE_PREFIX.length());
                IOpenMethod method = preBinderMethods.get(sprMethodName);
                if (method != null){
                    RecursiveOpenMethodPreBinder openMethodBinder = (RecursiveOpenMethodPreBinder) method;
                    if (openMethodBinder.isPreBinding()){
                        IOpenClass type = super.findType(namespace, typeName);
                        if (type != null){
                            return type;
                        }else{
                            throw new RecursiveMethodPreBindingException();
                        }
                    }
                    openMethodBinder.preBind();
                    preBinderMethods.remove(openMethodBinder.getHeader());
                }
            }
        }
        return super.findType(namespace, typeName);
    }
    
    public void addBinderMethod(OpenMethodHeader openMethodHeader, IOpenMethod method){
        preBinderMethods.put(openMethodHeader, method);
    }
    
    public void preBindMethod(OpenMethodHeader openMethodHeader){
        IOpenMethod method = preBinderMethods.get(openMethodHeader);
        if (method != null){
            RecursiveOpenMethodPreBinder openMethodBinder = (RecursiveOpenMethodPreBinder) method;
            openMethodBinder.preBind();
            preBinderMethods.remove(openMethodBinder.getHeader());
        }
    }
    
    public final class CurrentRuntimeContextMethod implements IOpenMethod {
        public final static  String CURRENT_CONTEXT_METHOD_NAME = "getContext";
        
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            IRulesRuntimeContext context = (IRulesRuntimeContext)env.getContext();
            if (context == null) {
                return null;
            }
            try {
                return context.clone();
            } catch (CloneNotSupportedException e) {
                log.warn("Failed to clone runtime context. Runtime context managing may work incorrectly.", e);
                return context;
            }
        }
        
        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return CURRENT_CONTEXT_METHOD_NAME;
        }

        public String getDisplayName(int mode) {
            return CURRENT_CONTEXT_METHOD_NAME;
        }

        public boolean isStatic() {
            return false;
        }

        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IRulesRuntimeContext.class);
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] {}, new String[] {});
        }
    }

    public final class EmptyRuntimeContextMethod implements IOpenMethod {
        public final static  String EMPTY_CONTEXT_METHOD_NAME = "emptyContext";

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return RulesRuntimeContextFactory.buildRulesRuntimeContext();
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return EMPTY_CONTEXT_METHOD_NAME;
        }

        public String getDisplayName(int mode) {
            return EMPTY_CONTEXT_METHOD_NAME;
        }

        public boolean isStatic() {
            return false;
        }

        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IRulesRuntimeContext.class);
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] {}, new String[] {});
        }
    }

    public final class RestoreRuntimeContextMethod implements IOpenMethod {
        public final static  String RESTORE_CONTEXT_METHOD_NAME = "restoreContext";

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if(env.isContextManagingSupported()){
                env.popContext();
            } else {
                log.warn("Failed to restore runtime context. Runtime context does not support context modifications.");
            }
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return RESTORE_CONTEXT_METHOD_NAME;
        }

        public String getDisplayName(int mode) {
            return RESTORE_CONTEXT_METHOD_NAME;
        }

        public boolean isStatic() {
            return false;
        }

        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] {}, new String[] {});
        }
    }

    public final class SetRuntimeContextMethod implements IOpenMethod {
        public final static  String SET_CONTEXT_METHOD_NAME = "setContext";

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if(env.isContextManagingSupported()){
                IRulesRuntimeContext runtimeContext =  (IRulesRuntimeContext)params[0];
                env.pushContext(runtimeContext);
            } else {
                log.warn("Failed to set runtime context. Runtime context does not support context modifications.");
            }
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return SET_CONTEXT_METHOD_NAME;
        }

        public String getDisplayName(int mode) {
            return SET_CONTEXT_METHOD_NAME;
        }

        public boolean isStatic() {
            return false;
        }

        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] { JavaOpenClass.getOpenClass(IRulesRuntimeContext.class) },
                new String[] { "context"});
        }
    }

    public final class ModifyRuntimeContextMethod implements IOpenMethod {
        public final static  String MODIFY_CONTEXT_METHOD_NAME = "modifyContext";

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if(env.isContextManagingSupported()){
                IRulesRuntimeContext runtimeContext = (IRulesRuntimeContext)env.getContext();
                if(runtimeContext == null){
                    runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
                }else{
                    runtimeContext = new RulesRuntimeContextDelegator(runtimeContext);
                }
                runtimeContext.setValue((String)params[0], params[1]);
                env.pushContext(runtimeContext);
            } else {
                log.warn("Failed to modify runtime context. Runtime context does not support context modifications.");
            }
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return MODIFY_CONTEXT_METHOD_NAME;
        }

        public String getDisplayName(int mode) {
            return MODIFY_CONTEXT_METHOD_NAME;
        }

        public boolean isStatic() {
            return false;
        }

        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.OBJECT },
                new String[] { "property", "value" });
        }
    }
}
