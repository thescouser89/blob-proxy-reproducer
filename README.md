# Proof of Concept to upload / download files in Quarkus

This project is a reproducer to show that `BlobProxy.generateProxy` doesn't work with input streams with lengths greater
than 2GB.

## Run
```bash
$ quarkus dev
```

Let's generate a file to upload.
```bash
# 3GB file
$ base64 /dev/urandom | head -c 3000000000 > file-3000m.txt
```

Let's upload!
```bash
$ curl -v -F file=@file-3000m.txt http://localhost:8080/hello
```

I would expect that `BlobProxy` can handle the 3GB file. Instead it throws this exception:
```
Caused by: java.lang.NegativeArraySizeException: -1294967296
        at org.postgresql.jdbc.PgPreparedStatement.createBlob(PgPreparedStatement.java:1208)
        at org.postgresql.jdbc.PgPreparedStatement.setBlob(PgPreparedStatement.java:1232)
        at io.agroal.pool.wrapper.PreparedStatementWrapper.setBlob(PreparedStatementWrapper.java:339)
        at org.hibernate.type.descriptor.jdbc.BlobJdbcType$4$1.doBind(BlobJdbcType.java:176)
        at org.hibernate.type.descriptor.jdbc.BasicBinder.bind(BasicBinder.java:61)
        at org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl.lambda$beforeStatement$0(JdbcValueBindingsImpl.java:87)
        at java.base/java.lang.Iterable.forEach(Iterable.java:75)
        at org.hibernate.engine.jdbc.mutation.spi.BindingGroup.forEachBinding(BindingGroup.java:51)
        at org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl.beforeStatement(JdbcValueBindingsImpl.java:85)
```

For sizes lower than 2GB, this works. I suspect that somewhere in `BlobProxy`, the size of type long is being cast into
an int and causing an Integer overflow.
