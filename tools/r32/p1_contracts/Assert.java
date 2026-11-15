package org.junit;
import java.util.*;
/** Host assertion API transport; every failed condition throws. Production logic is never stubbed. */
public class Assert {
    public static void fail(){throw new AssertionError();}
    public static void fail(String message){throw new AssertionError(message);}
    public static void assertTrue(boolean value){assertTrue("expected true",value);}
    public static void assertTrue(String message,boolean value){if(!value)fail(message);}
    public static void assertFalse(boolean value){assertFalse("expected false",value);}
    public static void assertFalse(String message,boolean value){if(value)fail(message);}
    public static void assertNull(Object value){assertNull("expected null",value);}
    public static void assertNull(String message,Object value){if(value!=null)fail(message+": "+value);}
    public static void assertNotNull(Object value){assertNotNull("expected nonnull",value);}
    public static void assertNotNull(String message,Object value){if(value==null)fail(message);}
    public static void assertEquals(Object want,Object got){assertEquals("object equality",want,got);}
    public static void assertEquals(String message,Object want,Object got){if(!Objects.equals(want,got))fail(message+": expected="+want+" actual="+got);}
    public static void assertEquals(long want,long got){assertEquals("integer equality",want,got);}
    public static void assertEquals(String message,long want,long got){if(want!=got)fail(message+": expected="+want+" actual="+got);}
    public static void assertEquals(double want,double got,double delta){assertEquals("floating equality",want,got,delta);}
    public static void assertEquals(String message,double want,double got,double delta){if(Double.compare(want,got)!=0&&!(Math.abs(want-got)<=delta))fail(message+": expected="+want+" actual="+got);}
    public static void assertNotEquals(Object want,Object got){assertNotEquals("expected different",want,got);}
    public static void assertNotEquals(String message,Object want,Object got){if(Objects.equals(want,got))fail(message);}
    public static void assertNotEquals(long want,long got){assertNotEquals("expected different",want,got);}
    public static void assertNotEquals(String message,long want,long got){if(want==got)fail(message);}
    public static void assertNotEquals(double want,double got,double delta){if(Double.compare(want,got)==0||Math.abs(want-got)<=delta)fail("expected different floating value");}
    public static void assertSame(Object want,Object got){assertSame("expected same object",want,got);}
    public static void assertSame(String message,Object want,Object got){if(want!=got)fail(message);}
    public static void assertNotSame(Object want,Object got){if(want==got)fail("expected different objects");}
    public static void assertNotSame(String message,Object want,Object got){if(want==got)fail(message);}
    public static void assertArrayEquals(Object[] want,Object[] got){assertArrayEquals("array equality",want,got);}
    public static void assertArrayEquals(String message,Object[] want,Object[] got){if(!Arrays.deepEquals(want,got))fail(message);}
    public static void assertArrayEquals(byte[] want,byte[] got){assertArrayEquals("byte array equality",want,got);}
    public static void assertArrayEquals(String message,byte[] want,byte[] got){if(!Arrays.equals(want,got))fail(message);}
    public static void assertArrayEquals(int[] want,int[] got){assertArrayEquals("integer array equality",want,got);}
    public static void assertArrayEquals(String message,int[] want,int[] got){if(!Arrays.equals(want,got))fail(message);}
    public static void assertArrayEquals(long[] want,long[] got){assertArrayEquals("long array equality",want,got);}
    public static void assertArrayEquals(String message,long[] want,long[] got){if(!Arrays.equals(want,got))fail(message);}
    public static void assertArrayEquals(boolean[] want,boolean[] got){if(!Arrays.equals(want,got))fail("boolean array equality");}
    public static void assertArrayEquals(double[] want,double[] got,double delta){assertArrayEquals("double array equality",want,got,delta);}
    public static void assertArrayEquals(String message,double[] want,double[] got,double delta){assertNotNull(message,want);assertNotNull(message,got);assertEquals(message,want.length,got.length);for(int i=0;i<want.length;i++)assertEquals(message+" at "+i,want[i],got[i],delta);}
    public static void assertArrayEquals(float[] want,float[] got,float delta){assertNotNull(want);assertNotNull(got);assertEquals(want.length,got.length);for(int i=0;i<want.length;i++)assertEquals(want[i],got[i],delta);}
    @FunctionalInterface public interface ThrowingRunnable { void run() throws Throwable; }
    public static <T extends Throwable>T assertThrows(Class<T> type,ThrowingRunnable runnable){return assertThrows("expected exception "+type.getName(),type,runnable);}
    public static <T extends Throwable>T assertThrows(String message,Class<T> type,ThrowingRunnable runnable){try{runnable.run();}catch(Throwable actual){if(type.isInstance(actual))return type.cast(actual);throw new AssertionError(message+" but got "+actual,actual);}throw new AssertionError(message+" but nothing was thrown");}
}
