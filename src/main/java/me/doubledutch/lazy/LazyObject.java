package me.doubledutch.lazy;

import java.util.Iterator;

/**
 * An object used to parse and inspect JSON data given in the form of a string.
 */
public class LazyObject{
	private LazyToken root;
	private char[] cbuf;

	// Cache value for length
	private int length=-1;

	/**
	 * Create a new Lazy JSON object based on the JSON representation in the given string.
	 *
	 * @param raw the input string
	 * @return a new LazyObject
	 * @throws LazyException if the string could not be parsed as a JSON object
	 */
	public LazyObject(String raw) throws LazyException{
		LazyParser parser=new LazyParser(raw);
		parser.tokenize();	
		if(parser.root.type!=LazyToken.OBJECT){
			throw new LazyException("JSON Object must start with {",0);
		}
		root=parser.root;
		cbuf=parser.cbuf;
		// source=raw;
	}

	protected LazyObject(LazyToken root,char[] source){
		this.root=root;
		this.cbuf=source;
	}

	/**
	 * Returns the string value stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return a boolean value
	 * @throws an exception if the value for the given key was not a string.
	 */
	public String getString(String key) throws LazyException{
		LazyToken token=getFieldToken(key);
		return token.getStringValue(cbuf);
	}

	/**
	 * Returns the integer value stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return a boolean value
	 * @throws an exception if the value for the given key was not an integer.
	 */
	public int getInt(String key) throws LazyException{
		LazyToken token=getFieldToken(key);
		return token.getIntValue(cbuf);
	}

	/**
	 * Returns the long value stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return a boolean value
	 * @throws an exception if the value for the given key was not a long.
	 */
	public long getLong(String key) throws LazyException{
		LazyToken token=getFieldToken(key);
		return token.getLongValue(cbuf);
	}

	/**
	 * Returns the double value stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return a boolean value
	 * @throws an exception if the value for the given key was not a double.
	 */
	public double getDouble(String key) throws LazyException{
		LazyToken token=getFieldToken(key);
		return token.getDoubleValue(cbuf);
	}

	/**
	 * Returns true if the value stored in this object for the given key is null.
	 *
	 * @param key the name of the field on this object
	 * @return true if the value is null, false otherwise
	 * @throws an exception if no value was set for the given key.
	 */
	public boolean isNull(String key){
		LazyToken token=getFieldToken(key);
		if(token.type==LazyToken.VALUE_NULL)return true;
		return false;
	}

	/**
	 * Returns the boolean value stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return a boolean value
	 * @throws an exception if the value for the given key was not a boolean.
	 */
	public boolean getBoolean(String key){
		LazyToken token=getFieldToken(key);
		if(token.type==LazyToken.VALUE_TRUE)return true;
		if(token.type==LazyToken.VALUE_FALSE)return false;
		throw new LazyException("Requested value is not a boolean",token);
	}

	/**
	 * Returns the JSON object stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return an array value
	 * @throws an exception if the value for the given key was not an object.
	 */
	public LazyObject getJSONObject(String key) throws LazyException{
		LazyToken token=getFieldToken(key);
		if(token.type!=LazyToken.OBJECT)throw new LazyException("Requested value is not an object",token);
		return new LazyObject(token,cbuf);
	}

	/**
	 * Returns the JSON array stored in this object for the given key.
	 *
	 * @param key the name of the field on this object
	 * @return an array value
	 * @throws an exception if the value for the given key was not an array.
	 */
	public LazyArray getJSONArray(String key) throws LazyException{
		LazyToken token=getFieldToken(key);
		if(token.type!=LazyToken.ARRAY)throw new LazyException("Requested value is not an array",token);
		return new LazyArray(token,cbuf);
	}

	/**
	 * Returns the number of fields on this object
	 *
	 * @return the number of fields
	 */
	public int length(){
		if(root.child==null){
			return 0;
		}
		if(length>-1){
			return length;
		}
		length=root.getChildCount();
		return length;
	}

	/**
	 * Returns a string iterator with the fields of this object as values.
	 *
	 * @return an iterator of object field names
	 */
	public Iterator<String> keys(){
		return root.getStringIterator(cbuf);
	}

	/**
	 * Utility method to evaluate wether a given string matches the value
	 * of a field.
	 *
	 * @param key the key to compare a token to
	 * @param token the field token
	 * @return true if the key matches, false otherwise
	 */
	private boolean keyMatch(String key,LazyToken token){
		// Quickly check the length first
		int length=key.length();
		if(token.endIndex-token.startIndex!=length){
			return false;
		}
		// Now go through the field character for character to compare
		for(int i=0;i<length;i++){
			char c=key.charAt(i);
			if(c!=cbuf[token.startIndex+i]){
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if the given key matches a field on this object.
	 *
	 * @param key the name of the field to look for
	 * @return true if the key exists, false otherwise
	 */
	public boolean has(String key){
		LazyToken child=root.child;
		while(child!=null){
			if(child.type==LazyToken.FIELD){
				if(keyMatch(key,child)){
					return true;
				}
			}
			child=child.next;
		}
		return false;
	}

	/**
	 * Fields for an object are attached as children on the token representing
	 * the object itself. This method finds the correct field for a given key
	 * and returns its first child - the child being the value for that field.
	 * This is a utility method used internally to extract field values.
	 *
	 * @param key the name of the desired field
	 * @return the first child of the matching field token if one exists
	 * @throws LazyException if the field does not exist
	 */
	private LazyToken getFieldToken(String key) throws LazyException{
		LazyToken child=root.child;
		while(child!=null){
			if(child.type==LazyToken.FIELD){
				if(keyMatch(key,child)){
					return child.child;
				}
			}else{
				// Only field tokens should be attached to an object token
				// This error would indicate a broken parser!
				throw new LazyException("Syntax error",child);
			}
			child=child.next;
		}
		throw new LazyException("Unknown field '"+key+"'");
	}

	/**
	 * Utility method to get the string value of a specific token
	 *
	 * @param token the token for which to extract a string
	 * @return the string value of the given token
	 */
	private String getString(LazyToken token){
		return token.getStringValue(cbuf);
	}

	/*
	// For debug purposes only
	public String toString(int pad){
		return root.toString(pad);
	}
	*/

	/**
	 * Returns a raw string extracted from the source string that covers the
	 * start and end index of this object.
	 *
	 * @return as string representation of this object as given in the source string
	 */
	public String toString(){
		return new String(cbuf,root.startIndex,root.endIndex-root.startIndex);
	}
}