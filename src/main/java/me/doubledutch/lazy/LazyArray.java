package me.doubledutch.lazy;

/**
 * An array used to parse and inspect JSON data given in the form of a string.
 */
public class LazyArray{
	private LazyToken root;
	private char[] cbuf;

	// Cache value for length
	private int length=-1;

	// Stored traversal location for fast in order traversals
	private LazyToken selectToken=null;
	private int selectInt=-1;

	/**
	 * Create a new Lazy JSON array based on the JSON representation in the given string.
	 *
	 * @param raw the input string
	 * @return a new LazyArray
	 * @throws LazyException if the string could not be parsed as a JSON array
	 */
	public LazyArray(String raw) throws LazyException{
		LazyParser parser=new LazyParser(raw);
		parser.tokenize();	
		if(parser.root.type!=LazyToken.ARRAY){
			throw new LazyException("JSON Array must start with [",0);
		}
		root=parser.root;
		cbuf=parser.cbuf;
	}

	protected LazyArray(LazyToken root,char[] source){
		this.root=root;
		this.cbuf=source;
	}

	/**
	 * Returns the number of values in this array
	 *
	 * @return the number of values
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
	 * Returns the JSON array stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as a JSON array
	 * @throws LazyException if the index is out of bounds
	 */
	public LazyArray getJSONArray(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		if(token.type!=LazyToken.ARRAY)throw new LazyException("Requested value is not an array",token);
		return new LazyArray(token,cbuf);
	}

	/**
	 * Returns the JSON object stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as a JSON object
	 * @throws LazyException if the index is out of bounds
	 */
	public LazyObject getJSONObject(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		if(token.type!=LazyToken.OBJECT)throw new LazyException("Requested value is not an object",token);
		return new LazyObject(token,cbuf);
	}

	/**
	 * Returns the boolean value stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as a boolean
	 * @throws LazyException if the index is out of bounds
	 */
	public boolean getBoolean(int index){
		LazyToken token=getValueToken(index);
		if(token.type==LazyToken.VALUE_TRUE)return true;
		if(token.type==LazyToken.VALUE_FALSE)return false;
		throw new LazyException("Requested value is not a boolean",token);
	}

	/**
	 * Returns the string value stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as a string
	 * @throws LazyException if the index is out of bounds
	 */
	public String getString(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		return token.getStringValue(cbuf);
	}

	/**
	 * Returns the int value stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as an int
	 * @throws LazyException if the index is out of bounds
	 */
	public int getInt(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		return token.getIntValue(cbuf);
	}

	/**
	 * Returns the long value stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as a long
	 * @throws LazyException if the index is out of bounds
	 */
	public long getLong(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		return token.getLongValue(cbuf);
	}

	/**
	 * Returns the double value stored at the given index.
	 *
	 * @param index the location of the value in this array
	 * @return the value if it could be parsed as a double
	 * @throws LazyException if the index is out of bounds
	 */
	public double getDouble(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		return token.getDoubleValue(cbuf);
	}

	/**
	 * Returns true if the value stored at the given index is null.
	 *
	 * @param index the location of the value in this array
	 * @return true if the value is null, false otherwise
	 * @throws LazyException if the index is out of bounds
	 */
	public boolean isNull(int index) throws LazyException{
		LazyToken token=getValueToken(index);
		if(token.type==LazyToken.VALUE_NULL)return true;
		return false;
	}

	/**
	 * Values for an array are attached as children on the token representing
	 * the array itself. This method finds the correct child for a given index
	 * and returns it.
	 *
	 * Since children are stored as a linked list, this method is likely to be
	 * a serious O(n) performance bottleneck for array access. To improve this
	 * for the most common case, we maintain a traversal index and pointer into
	 * the children - meaning that if you traverse the array from the beginning
	 * the complexity will be O(1) for each access request instead.
	 *
	 * @param index the location of the desired value
	 * @return the child for the given index
	 * @throws LazyException if the index is out of bounds
	 */
	private LazyToken getValueToken(int index) throws LazyException{
		if(index<0)throw new LazyException("Array undex can not be negative");
		int num=0;
		LazyToken child=root.child;
		// If the value we are looking for is past our previous traversal point
		// continue at the previous point
		if(selectInt>-1 && index>=selectInt){
			num=selectInt;
			child=selectToken;
		}
		while(child!=null){
			if(num==index){
				// Store the traversal point and return the current token
				selectInt=index;
				selectToken=child;
				return child;
			}
			num++;
			child=child.next;
		}
		throw new LazyException("Array index out of bounds "+index);
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

	/**
	 * Returns a raw string extracted from the source string that covers the
	 * start and end index of this object.
	 *
	 * @return as string representation of this object as given in the source string
	 */
	public String toString(){
		return new String(cbuf,root.startIndex,root.endIndex-root.startIndex);
	}

	/*
	// For debug purposes only
	public String toString(int pad){
		return root.toString(pad);
	}
	*/
}