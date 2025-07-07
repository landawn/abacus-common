package com.landawn.abacus.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.exception.UncheckedParseException;
import com.landawn.abacus.exception.UncheckedReflectiveOperationException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.u.Optional;

public class ExceptionUtil100Test extends TestBase {
    
    // Custom test exceptions
    public static class CustomCheckedException extends Exception {
        public CustomCheckedException(String message) {
            super(message);
        }
        
        public CustomCheckedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class CustomRuntimeException extends RuntimeException {
        public CustomRuntimeException(String message) {
            super(message);
        }
        
        public CustomRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    

    
    @Test
    public void testRegisterRuntimeExceptionMapperNullArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            ExceptionUtil.registerRuntimeExceptionMapper(null, e -> new RuntimeException())
        );
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            ExceptionUtil.registerRuntimeExceptionMapper(CustomCheckedException.class, null)
        );
    }
    
    @Test
    public void testRegisterRuntimeExceptionMapperBuiltinClass() {
        // Should not allow registration for built-in classes
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            ExceptionUtil.registerRuntimeExceptionMapper(IOException.class, e -> new RuntimeException(e))
        );
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            ExceptionUtil.registerRuntimeExceptionMapper(RuntimeException.class, e -> e)
        );
    }
    
    @Test
    public void testToRuntimeExceptionWithException() {
        // Test with RuntimeException - should return itself
        RuntimeException runtimeEx = new RuntimeException("runtime");
        Assertions.assertSame(runtimeEx, ExceptionUtil.toRuntimeException(runtimeEx));
        
        // Test with IOException
        IOException ioEx = new IOException("io error");
        RuntimeException result = ExceptionUtil.toRuntimeException(ioEx);
        Assertions.assertTrue(result instanceof UncheckedIOException);
        Assertions.assertEquals(ioEx, result.getCause());
        
        // Test with SQLException
        SQLException sqlEx = new SQLException("sql error", "42", 1054);
        result = ExceptionUtil.toRuntimeException(sqlEx);
        Assertions.assertTrue(result instanceof UncheckedSQLException);
        Assertions.assertEquals(sqlEx, result.getCause());
        
        // Test with ParseException
        ParseException parseEx = new ParseException("parse error", 10);
        result = ExceptionUtil.toRuntimeException(parseEx);
        Assertions.assertTrue(result instanceof UncheckedParseException);
        Assertions.assertEquals(parseEx, result.getCause());
        
        // Test with InterruptedException
        InterruptedException interruptEx = new InterruptedException("interrupted");
        result = ExceptionUtil.toRuntimeException(interruptEx);
        Assertions.assertTrue(result instanceof UncheckedInterruptedException);
        Assertions.assertEquals(interruptEx, result.getCause());
        
        // Test with ReflectiveOperationException
        ReflectiveOperationException reflectiveEx = new ReflectiveOperationException("reflective error");
        result = ExceptionUtil.toRuntimeException(reflectiveEx);
        Assertions.assertTrue(result instanceof UncheckedReflectiveOperationException);
        Assertions.assertEquals(reflectiveEx, result.getCause());
        
        // Test with generic checked exception
        Exception genericEx = new Exception("generic");
        result = ExceptionUtil.toRuntimeException(genericEx);
        Assertions.assertTrue(result instanceof UncheckedException);
        Assertions.assertEquals(genericEx, result.getCause());
    }
    
    @Test
    public void testToRuntimeExceptionWithCallInterrupt() {
        // Clear interrupt status first
        Thread.interrupted();
        
        InterruptedException interruptEx = new InterruptedException("interrupted");
        RuntimeException result = ExceptionUtil.toRuntimeException(interruptEx, true);
        
        Assertions.assertTrue(result instanceof UncheckedInterruptedException);
        Assertions.assertTrue(Thread.interrupted()); // Check and clear interrupt status
        
        // Test with non-InterruptedException
        IOException ioEx = new IOException("io error");
        result = ExceptionUtil.toRuntimeException(ioEx, true);
        Assertions.assertTrue(result instanceof UncheckedIOException);
        Assertions.assertFalse(Thread.interrupted()); // Should not be set
    }
    
    @Test
    public void testToRuntimeExceptionWithThrowable() {
        // Test with Error
        Error error = new Error("error");
        RuntimeException result = ExceptionUtil.toRuntimeException(error);
        Assertions.assertTrue(result instanceof RuntimeException);
        Assertions.assertEquals("error", result.getCause().getMessage());
        
        // Test with ExecutionException
        IOException cause = new IOException("io cause");
        ExecutionException execEx = new ExecutionException(cause);
        result = ExceptionUtil.toRuntimeException(execEx);
        Assertions.assertTrue(result instanceof UncheckedIOException);
        Assertions.assertEquals(cause, result.getCause());
        
        // Test with ExecutionException without cause
        ExecutionException execExNoCause = new ExecutionException(null);
        result = ExceptionUtil.toRuntimeException(execExNoCause);
        Assertions.assertTrue(result instanceof UncheckedException);
        Assertions.assertEquals(execExNoCause, result.getCause());
        
        // Test with InvocationTargetException
        InvocationTargetException invocEx = new InvocationTargetException(cause);
        result = ExceptionUtil.toRuntimeException(invocEx);
        Assertions.assertTrue(result instanceof UncheckedIOException);
        Assertions.assertEquals(cause, result.getCause());
        
        // Test with UndeclaredThrowableException
        UndeclaredThrowableException undeclaredEx = new UndeclaredThrowableException(cause);
        result = ExceptionUtil.toRuntimeException(undeclaredEx);
        Assertions.assertTrue(result instanceof UncheckedIOException);
        Assertions.assertEquals(cause, result.getCause());
    }
    
    @Test
    public void testToRuntimeExceptionWithThrowIfItIsError() {
        Error error = new Error("test error");
        
        // With throwIfItIsError = true, should throw the error
        Assertions.assertThrows(Error.class, () -> 
            ExceptionUtil.toRuntimeException(error, false, true)
        );
        
        // With throwIfItIsError = false, should wrap the error
        RuntimeException result = ExceptionUtil.toRuntimeException(error, false, false);
        Assertions.assertTrue(result instanceof RuntimeException);
        Assertions.assertEquals(error, result.getCause());
    }
    
    @Test
    public void testTryToGetOriginalCheckedException() {
        // Test with UncheckedException
        IOException originalCause = new IOException("original");
        UncheckedException uncheckedException = new UncheckedException(originalCause);
        Exception result = ExceptionUtil.tryToGetOriginalCheckedException(uncheckedException);
        Assertions.assertEquals(originalCause, result);
        
        // Test with custom unchecked exception
        CustomCheckedException customCause = new CustomCheckedException("custom");
        RuntimeException customUnchecked = new UncheckedException(customCause);
        result = ExceptionUtil.tryToGetOriginalCheckedException(customUnchecked);
        Assertions.assertEquals(customCause, result);
        
        // Test with ExecutionException chain
        ExecutionException execEx = new ExecutionException(new InvocationTargetException(originalCause));
        result = ExceptionUtil.tryToGetOriginalCheckedException(execEx);
        Assertions.assertEquals(originalCause, result);
        
        // Test with RuntimeException with RuntimeException cause
        RuntimeException runtimeEx = new RuntimeException(new RuntimeException("runtime cause"));
        result = ExceptionUtil.tryToGetOriginalCheckedException(runtimeEx);
        Assertions.assertEquals(runtimeEx, result);
        
        // Test with no cause
        RuntimeException noCause = new RuntimeException("no cause");
        result = ExceptionUtil.tryToGetOriginalCheckedException(noCause);
        Assertions.assertEquals(noCause, result);
    }
    
    @Test
    public void testHasCauseWithClass() {
        // Create exception chain
        IOException ioEx = new IOException("io");
        SQLException sqlEx = new SQLException("sql", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime", sqlEx);
        
        // Test direct match
        Assertions.assertTrue(ExceptionUtil.hasCause(runtimeEx, RuntimeException.class));
        Assertions.assertTrue(ExceptionUtil.hasCause(runtimeEx, SQLException.class));
        Assertions.assertTrue(ExceptionUtil.hasCause(runtimeEx, IOException.class));
        
        // Test no match
        Assertions.assertFalse(ExceptionUtil.hasCause(runtimeEx, ParseException.class));
        
        // Test with null cause
        RuntimeException noCause = new RuntimeException("no cause");
        Assertions.assertTrue(ExceptionUtil.hasCause(noCause, RuntimeException.class));
        Assertions.assertFalse(ExceptionUtil.hasCause(noCause, IOException.class));
        
        // Test with circular reference
        RuntimeException circular1 = new RuntimeException("circular1");
        RuntimeException circular2 = new RuntimeException("circular2", circular1);
        try {
            // Use reflection to create circular reference
            Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(circular1, circular2);
            
            Assertions.assertTrue(ExceptionUtil.hasCause(circular1, RuntimeException.class));
        } catch (Exception e) {
            // Skip test if reflection fails
        }
    }
    
    @Test
    public void testHasCauseWithPredicate() {
        IOException ioEx = new IOException("io error");
        SQLException sqlEx = new SQLException("sql error", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime error", sqlEx);
        
        // Test with message predicate
        Predicate<Throwable> containsError = ex -> ex.getMessage() != null && ex.getMessage().contains("error");
        Assertions.assertTrue(ExceptionUtil.hasCause(runtimeEx, containsError));
        
        // Test with type predicate
        Predicate<Throwable> isIOException = ex -> ex instanceof IOException;
        Assertions.assertTrue(ExceptionUtil.hasCause(runtimeEx, isIOException));
        
        // Test with no match
        Predicate<Throwable> containsFoo = ex -> ex.getMessage() != null && ex.getMessage().contains("foo");
        Assertions.assertFalse(ExceptionUtil.hasCause(runtimeEx, containsFoo));
    }
    
    @Test
    public void testHasSQLCause() {
        // Test with direct SQLException
        SQLException sqlEx = new SQLException("sql error");
        Assertions.assertTrue(ExceptionUtil.hasSQLCause(sqlEx));
        
        // Test with wrapped SQLException
        RuntimeException runtimeEx = new RuntimeException("runtime", sqlEx);
        Assertions.assertTrue(ExceptionUtil.hasSQLCause(runtimeEx));
        
        // Test with UncheckedSQLException
        UncheckedSQLException uncheckedSqlEx = new UncheckedSQLException(sqlEx);
        Assertions.assertTrue(ExceptionUtil.hasSQLCause(uncheckedSqlEx));
        
        // Test with no SQL cause
        IOException ioEx = new IOException("io error");
        Assertions.assertFalse(ExceptionUtil.hasSQLCause(ioEx));
    }
    
    @Test
    public void testHasIOCause() {
        // Test with direct IOException
        IOException ioEx = new IOException("io error");
        Assertions.assertTrue(ExceptionUtil.hasIOCause(ioEx));
        
        // Test with wrapped IOException
        RuntimeException runtimeEx = new RuntimeException("runtime", ioEx);
        Assertions.assertTrue(ExceptionUtil.hasIOCause(runtimeEx));
        
        // Test with UncheckedIOException
        UncheckedIOException uncheckedIoEx = new UncheckedIOException(ioEx);
        Assertions.assertTrue(ExceptionUtil.hasIOCause(uncheckedIoEx));
        
        // Test with no IO cause
        SQLException sqlEx = new SQLException("sql error");
        Assertions.assertFalse(ExceptionUtil.hasIOCause(sqlEx));
    }
    
    @Test
    public void testIsNullPointerOrIllegalArgumentException() {
        NullPointerException npe = new NullPointerException("null");
        Assertions.assertTrue(ExceptionUtil.isNullPointerOrIllegalArgumentException(npe));
        
        IllegalArgumentException iae = new IllegalArgumentException("illegal");
        Assertions.assertTrue(ExceptionUtil.isNullPointerOrIllegalArgumentException(iae));
        
        IOException ioEx = new IOException("io");
        Assertions.assertFalse(ExceptionUtil.isNullPointerOrIllegalArgumentException(ioEx));
        
        RuntimeException runtimeEx = new RuntimeException("runtime");
        Assertions.assertFalse(ExceptionUtil.isNullPointerOrIllegalArgumentException(runtimeEx));
    }
    
    @Test
    public void testListCause() {
        // Create exception chain
        IOException ioEx = new IOException("io");
        SQLException sqlEx = new SQLException("sql", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime", sqlEx);
        
        List<Throwable> causes = ExceptionUtil.listCause(runtimeEx);
        Assertions.assertEquals(3, causes.size());
        Assertions.assertEquals(runtimeEx, causes.get(0));
        Assertions.assertEquals(sqlEx, causes.get(1));
        Assertions.assertEquals(ioEx, causes.get(2));
        
        // Test with no cause
        RuntimeException noCause = new RuntimeException("no cause");
        causes = ExceptionUtil.listCause(noCause);
        Assertions.assertEquals(1, causes.size());
        Assertions.assertEquals(noCause, causes.get(0));
        
        // Test with circular reference
        RuntimeException circular1 = new RuntimeException("circular1");
        RuntimeException circular2 = new RuntimeException("circular2", circular1);
        try {
            // Use reflection to create circular reference
            Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(circular1, circular2);
            
            causes = ExceptionUtil.listCause(circular1);
            // Should handle circular reference gracefully
            Assertions.assertTrue(causes.size() <= 3);
        } catch (Exception e) {
            // Skip test if reflection fails
        }
    }
    
    @Test
    public void testFirstCause() {
        // Create exception chain
        IOException ioEx = new IOException("io");
        SQLException sqlEx = new SQLException("sql", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime", sqlEx);
        
        Throwable firstCause = ExceptionUtil.firstCause(runtimeEx);
        Assertions.assertEquals(ioEx, firstCause);
        
        // Test with no cause
        RuntimeException noCause = new RuntimeException("no cause");
        firstCause = ExceptionUtil.firstCause(noCause);
        Assertions.assertEquals(noCause, firstCause);
        
        // Test with single cause
        RuntimeException singleCause = new RuntimeException("single", ioEx);
        firstCause = ExceptionUtil.firstCause(singleCause);
        Assertions.assertEquals(ioEx, firstCause);
    }
    
    @Test
    public void testFindCauseByClass() {
        // Create exception chain
        IOException ioEx = new IOException("io");
        SQLException sqlEx = new SQLException("sql", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime", sqlEx);
        
        // Test finding each exception in the chain
        Optional<RuntimeException> foundRuntime = ExceptionUtil.findCause(runtimeEx, RuntimeException.class);
        Assertions.assertTrue(foundRuntime.isPresent());
        Assertions.assertEquals(runtimeEx, foundRuntime.get());
        
        Optional<SQLException> foundSql = ExceptionUtil.findCause(runtimeEx, SQLException.class);
        Assertions.assertTrue(foundSql.isPresent());
        Assertions.assertEquals(sqlEx, foundSql.get());
        
        Optional<IOException> foundIo = ExceptionUtil.findCause(runtimeEx, IOException.class);
        Assertions.assertTrue(foundIo.isPresent());
        Assertions.assertEquals(ioEx, foundIo.get());
        
        // Test not found
        Optional<ParseException> notFound = ExceptionUtil.findCause(runtimeEx, ParseException.class);
        Assertions.assertFalse(notFound.isPresent());
    }
    
    @Test
    public void testFindCauseByPredicate() {
        IOException ioEx = new IOException("io error");
        SQLException sqlEx = new SQLException("sql error", ioEx);
        RuntimeException runtimeEx = new RuntimeException("runtime error", sqlEx);
        
        // Test finding by message content
        Optional<Throwable> foundIo = ExceptionUtil.findCause(runtimeEx, 
            ex -> ex.getMessage() != null && ex.getMessage().contains("io"));
        Assertions.assertTrue(foundIo.isPresent());
        Assertions.assertEquals(ioEx, foundIo.get());
        
        // Test finding by type
        Optional<Throwable> foundSql = ExceptionUtil.findCause(runtimeEx, 
            ex -> ex instanceof SQLException);
        Assertions.assertTrue(foundSql.isPresent());
        Assertions.assertEquals(sqlEx, foundSql.get());
        
        // Test not found
        Optional<Throwable> notFound = ExceptionUtil.findCause(runtimeEx, 
            ex -> ex.getMessage() != null && ex.getMessage().contains("foo"));
        Assertions.assertFalse(notFound.isPresent());
    }
    
    @Test
    public void testGetStackTrace() {
        // Test with exception
        Exception ex = new Exception("test exception");
        String stackTrace = ExceptionUtil.getStackTrace(ex);
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.contains("test exception"));
        Assertions.assertTrue(stackTrace.contains("Exception"));
        Assertions.assertTrue(stackTrace.contains("at "));
        
        // Test with null
        stackTrace = ExceptionUtil.getStackTrace(null);
        Assertions.assertEquals("", stackTrace);
        
        // Test with cause
        IOException cause = new IOException("cause");
        RuntimeException withCause = new RuntimeException("wrapper", cause);
        stackTrace = ExceptionUtil.getStackTrace(withCause);
        Assertions.assertTrue(stackTrace.contains("wrapper"));
        Assertions.assertTrue(stackTrace.contains("cause"));
        Assertions.assertTrue(stackTrace.contains("Caused by"));
    }
    
    @Test
    public void testGetErrorMessage() {
        // Test with message
        Exception ex = new Exception("error message");
        String msg = ExceptionUtil.getErrorMessage(ex);
        Assertions.assertEquals("error message", msg);
        
        // Test with no message
        Exception noMsg = new Exception();
        msg = ExceptionUtil.getErrorMessage(noMsg);
        Assertions.assertEquals(noMsg.getClass().getCanonicalName(), msg);
        
        // Test with cause that has message
        Exception cause = new Exception("cause message");
        Exception wrapper = new Exception(cause);
        msg = ExceptionUtil.getErrorMessage(wrapper);
        Assertions.assertEquals("java.lang.Exception: cause message", msg);
        
        // Test with SQLException
        SQLException sqlEx = new SQLException("sql error", "42", 1054);
        msg = ExceptionUtil.getErrorMessage(sqlEx);
        Assertions.assertEquals("1054|sql error", msg);
    }
    
    @Test
    public void testGetErrorMessageWithExceptionClassName() {
        // Test with message
        Exception ex = new Exception("error message");
        String msg = ExceptionUtil.getErrorMessage(ex, true);
        Assertions.assertEquals("Exception|error message", msg);
        
        // Test with no message
        Exception noMsg = new Exception();
        msg = ExceptionUtil.getErrorMessage(noMsg, true);
        Assertions.assertEquals("Exception|" + noMsg.getClass().getCanonicalName(), msg);
        
        // Test with SQLException
        SQLException sqlEx = new SQLException("sql error", "42", 1054);
        msg = ExceptionUtil.getErrorMessage(sqlEx, true);
        Assertions.assertEquals("SQLException|1054|sql error", msg);
        
        // Test with cause
        Exception cause = new Exception("cause message");
        Exception wrapper = new Exception(cause);
        msg = ExceptionUtil.getErrorMessage(wrapper, true);
        Assertions.assertEquals("Exception|java.lang.Exception: cause message", msg);
    }
    
    @Test
    public void testComplexExceptionChains() {
        // Test deep exception chain
        IOException level3 = new IOException("level 3");
        SQLException level2 = new SQLException("level 2", level3);
        RuntimeException level1 = new RuntimeException("level 1", level2);
        ExecutionException level0 = new ExecutionException("level 0", level1);
        
        // Test finding at different levels
        Optional<IOException> foundIo = ExceptionUtil.findCause(level0, IOException.class);
        Assertions.assertTrue(foundIo.isPresent());
        Assertions.assertEquals(level3, foundIo.get());
        
        // Test first cause
        Throwable first = ExceptionUtil.firstCause(level0);
        Assertions.assertEquals(level3, first);
        
        // Test list cause
        List<Throwable> causes = ExceptionUtil.listCause(level0);
        Assertions.assertEquals(4, causes.size());
    }
    
    @Test
    public void testCachingBehavior() {
        // Test that multiple conversions of same exception type use cached mapper
        IOException io1 = new IOException("io1");
        IOException io2 = new IOException("io2");
        
        RuntimeException result1 = ExceptionUtil.toRuntimeException(io1);
        RuntimeException result2 = ExceptionUtil.toRuntimeException(io2);
        
        Assertions.assertTrue(result1 instanceof UncheckedIOException);
        Assertions.assertTrue(result2 instanceof UncheckedIOException);
        Assertions.assertEquals(io1, result1.getCause());
        Assertions.assertEquals(io2, result2.getCause());
    }
}