package org.junit; /** HOST-only JUnit4 subset so plan tests run without Maven; the app uses real junit:4.13.2. */
public class Assert { public static void fail(String m){throw new AssertionError(m);} public static void assertTrue(boolean c){if(!c)fail("expected true");} public static void assertTrue(String m,boolean c){if(!c)fail(m);}
 public static void assertFalse(boolean c){if(c)fail("expected false");} public static void assertFalse(String m,boolean c){if(c)fail(m);}
 public static void assertNull(Object o){if(o!=null)fail("expected null but "+o);} public static void assertNull(String m,Object o){if(o!=null)fail(m+": "+o);} public static void assertNotNull(Object o){if(o==null)fail("expected non-null");}
 public static void assertEquals(Object a,Object b){if(a==null?b!=null:!a.equals(b))fail("expected "+a+" but "+b);} public static void assertEquals(String m,Object a,Object b){if(a==null?b!=null:!a.equals(b))fail(m+": expected "+a+" but "+b);}
 public static void assertEquals(long a,long b){if(a!=b)fail("expected "+a+" but "+b);} public static void assertEquals(String m,long a,long b){if(a!=b)fail(m+": expected "+a+" but "+b);}
 public static void assertEquals(double a,double b,double d){if(Math.abs(a-b)>d)fail("expected "+a+" but "+b);} public static void assertEquals(String m,double a,double b,double d){if(Math.abs(a-b)>d)fail(m+": expected "+a+" but "+b);}
 public static void assertNotEquals(Object a,Object b){if(a==null?b==null:a.equals(b))fail("expected different: "+a);} }
