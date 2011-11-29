/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests the fetching system for simple parent values.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FetchParentTests extends TestBase
{
	/** */
	@Entity
	public static class Father {
		public @Id Long id;
		public String foo;
	}
	
	/** */
	@Entity
	public static class Child {
		public @Id Long id;
		public @Load @Parent Father father;
		public String bar;
	}
	
	/** */
	@Test
	public void testParentExists() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);
		
		TestObjectify ofy = fact.begin();
		
		Father f = new Father();
		f.foo = "foo";
		ofy.put(f);
		
		Child ch = new Child();
		ch.father = f;
		ch.bar = "bar";
		ofy.put(ch);
		
		Child fetched = ofy.get(fact.<Child>getKey(ch));
		
		assert fetched.bar.equals(ch.bar);
		assert fetched.father.id.equals(f.id);
		assert fetched.father.foo.equals(f.foo);
	}

	/** */
	@Entity
	public static class TreeNode {
		public @Id Long id;
		public @Load @Parent TreeNode parent;
		public String foo;
	}
	
	/** */
	@Test
	public void testTwoLevelsOfFetch() throws Exception
	{
		fact.register(TreeNode.class);
		
		TestObjectify ofy = fact.begin();
		
		TreeNode node1 = new TreeNode();
		node1.foo = "foo1";
		ofy.put(node1);
		
		TreeNode node2 = new TreeNode();
		node2.parent = node1;
		node2.foo = "foo2";
		ofy.put(node2);
		
		TreeNode node3 = new TreeNode();
		node3.parent = node2;
		node3.foo = "foo3";
		ofy.put(node3);

		TreeNode fetched3 = ofy.get(fact.<TreeNode>getKey(node3));
		
		assert fetched3.foo.equals(node3.foo);
		assert fetched3.parent.id.equals(node2.id);
		assert fetched3.parent.foo.equals(node2.foo);
		assert fetched3.parent.parent.id.equals(node1.id);
		assert fetched3.parent.parent.foo.equals(node1.foo);
		assert fetched3.parent.parent.parent == null;
	}

	/** */
	@Test
	public void testMissingIntermediate() throws Exception
	{
		fact.register(TreeNode.class);
		
		TestObjectify ofy = fact.begin();
		
		TreeNode node1 = new TreeNode();
		node1.foo = "foo1";
		ofy.put(node1);

		// Node2 should not exist but should have a concrete id for node3
		TreeNode node2 = new TreeNode();
		node2.id = 999L;
		node2.parent = node1;
		
		TreeNode node3 = new TreeNode();
		node3.parent = node2;
		node3.foo = "foo3";
		ofy.put(node3);

		TreeNode fetched3 = ofy.get(fact.<TreeNode>getKey(node3));
		
		assert fetched3.parent.id.equals(node2.id);
		assert fetched3.parent.foo == null;
		assert fetched3.parent.parent.id.equals(node1.id);
		assert fetched3.parent.parent.foo.equals(node1.foo);
		assert fetched3.parent.parent.parent == null;
	}
	
	/** */
	@Entity
	public static class ChildWithGroup {
		public @Id Long id;
		public @Load("group") @Parent Father father;
		public String bar;
	}
	
	/** */
	@Test
	public void testParentWithGroup() throws Exception
	{
		fact.register(Father.class);
		fact.register(ChildWithGroup.class);
		
		TestObjectify ofy = fact.begin();
		
		Father f = new Father();
		f.foo = "foo";
		ofy.put(f);
		
		ChildWithGroup ch = new ChildWithGroup();
		ch.father = f;
		ch.bar = "bar";
		ofy.put(ch);
		
		// This should get a hollow entity
		Child fetched = ofy.get(fact.<Child>getKey(ch));
		assert fetched.father.id.equals(f.id);
		assert fetched.father.foo == null;

		// This should get the complete parent
		Child fetched2 = ofy.load().group("group").key(fact.<Child>getKey(ch)).get();
		assert fetched2.father.id.equals(f.id);
		assert fetched2.father.foo.equals(f.foo);
	}
	
}