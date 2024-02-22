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
}
