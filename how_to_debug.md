## There is an exception occurring randomly during running full unit tests.
For example: connection obtained from connection pool is closed.

* Step one: in `Connection.close()` method:

```
// log the how/where the connection is closed.
public class Connection {
	...
	private Exception lastStackTrace;
	...

	public void close() {
		...
		lastStackTrace = new RuntimeException();
	}

	public boolean isClosed() {
		...
	}

	public void printStackTrace() {
		lastStackTrace.printStackTrace();
	}
...
}
```

* Step two: in `Caller` method:

```
public class SomeClass {
	...
	public void caller() {
		Connection conn = getConnection(); // from pool

		// add below log, so you know where/how the Connection was closed.
		if (conn.isClosed()) {
			conn.printStackTrace();
			Thread.sleep(1000)
			System.exit(1);
		}
	}
	...
}
```
