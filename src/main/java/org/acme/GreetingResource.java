package org.acme;

import io.quarkus.logging.Log;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.engine.jdbc.BlobProxy;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Path("/hello")
public class GreetingResource {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public String hello(@RestForm(FileUpload.ALL) List<FileUpload> uploads) throws Exception {

        for (FileUpload upload: uploads) {
           Log.infof("Filename %s", upload.fileName());
            Log.infof("Size %s", upload.size());
            Log.infof("Size %s", upload.uploadedFile());

            MyEntity myEntity = new MyEntity();
            java.nio.file.Path path = upload.filePath();
            myEntity.blob = BlobProxy.generateProxy(Files.newInputStream(path, StandardOpenOption.READ), upload.size());
            myEntity.persist();
        }

        return "haa";
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response getblob(@PathParam("id") int id) throws Exception {
        MyEntity entity = MyEntity.findById(id);
        /**
         * Since we need to load all the data of the blob in a transaction, we write it to a temp file first, then
         * stream the content of the file in the response. Finally we delete the temp file for cleanup
         *
         * Strangely, we cannot stream the blob's input stream to the outputstream since the transaction is dead at this
         * point and the streaming from db to quarkus is dead. We instead have to save it into a temp file as an
         * intermediate
         */
        java.nio.file.Path path = Files.createTempFile("temp-upload-file", "out");
        FileOutputStream outputStream = new FileOutputStream(path.toFile());

        // copy the data of the blob to the temp file
        entity.blob.getBinaryStream().transferTo(outputStream);

        return Response.ok().entity((StreamingOutput) output -> {
            try {
                Files.copy(path, output);
            } finally {
                Files.delete(path);
            }
        }).build();
    }

    @GET
    @Path("/md5/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String getMd5blob(@PathParam("id") int id) throws Exception {
        MyEntity entity = MyEntity.findById(id);
        return DigestUtils.md5Hex(entity.blob.getBinaryStream());
    }
}
