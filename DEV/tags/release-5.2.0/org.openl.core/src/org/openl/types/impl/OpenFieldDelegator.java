/*
 * Created on Jul 1, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class OpenFieldDelegator implements IOpenField
{
	protected IOpenField field;
	
	public OpenFieldDelegator(IOpenField field)
	{
		this.field = field;
	}



  /**
   * @param target
   * @return
   */
  public Object get(Object target, IRuntimeEnv env)
  {
    return field.get(target, env);
  }

  /**
   * @return
   */
  public IOpenClass getDeclaringClass()
  {
    return field.getDeclaringClass();
  }

  /**
   * @return
   */
  public IMemberMetaInfo getInfo()
  {
    return field.getInfo();
  }

  /**
   * @return
   */
  public String getName()
  {
    return field.getName();
  }

  /**
   * @return
   */
  public IOpenClass getType()
  {
    return field.getType();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    return field.hashCode();
  }

  /**
   * @return
   */
  public boolean isConst()
  {
    return field.isConst();
  }

  /**
   * @return
   */
  public boolean isReadable()
  {
    return field.isReadable();
  }

  /**
   * @return
   */
  public boolean isStatic()
  {
    return field.isStatic();
  }

  /**
   * @return
   */
  public boolean isWritable()
  {
    return field.isWritable();
  }

  /**
   * @param target
   * @param value
   */
  public void set(Object target, Object value, IRuntimeEnv env)
  {
    field.set(target, value, env);
  }

  public String toString()
  {
    return field.toString();
  }



	public String getDisplayName(int mode)
	{
		return field.getDisplayName(mode);
	}



	@Override
	public boolean equals(Object obj)
	{
	    if (obj == null)
		return false;
	    if (obj == this)
		return true;
	    
	    if (obj instanceof OpenFieldDelegator)
	    {
		OpenFieldDelegator d = (OpenFieldDelegator) obj;
		return field.equals(d.field);
	    }
	    
	    if (obj instanceof IOpenField)
	    {
		IOpenField f = (IOpenField) obj;
		return field.equals(f);
	    }
	    
	    return super.equals(obj);
	}



	public IOpenField getField()
	{
	    return field;
	}

}
