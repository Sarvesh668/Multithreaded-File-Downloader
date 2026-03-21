package com.example.filedowloader.demo.service;

import java.io.File;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.filedowloader.demo.event.DownloadCompleteEvent;
import com.example.filedowloader.demo.model.DownloadTask;
import com.example.filedowloader.demo.model.Role;

@Service
public class CompressionService {

    private static final Logger log= LoggerFactory.getLogger(CompressionService.class);

    @EventListener
    public void handleDownloadComplete(DownloadCompleteEvent event){

        DownloadTask task= event.getTask();
        log.info("CompressionService woke up for TASK ID: {}", task.getId());

        String originalName= task.getOriginalFileName();
        String sourceFilePath = task.getSaveDirectory()+originalName;
        String destFilePath= task.getSaveDirectory()+originalName+"_compressed_"+task.getId()+".pdf";

        File sourceFile= new File(sourceFilePath);

        if(!sourceFile.exists()){
            log.error("Source file not found for compression: {}", sourceFilePath);
            return;
        }

        try{

            try(PDDocument document = Loader.loadPDF(sourceFile)){

                if(task.getUser().getRole()== Role.REGISTERED){
                    log.info("User is Registered. Applying MAXIMUM (100%) compression...");
                    applyMaxCompression(document);
                    document.save(new File(destFilePath),CompressParameters.DEFAULT_COMPRESSION);
                }
                else{
                    log.info("User is Guest. Applying STANDARD (70%) compression");
                    applyStandardCompression(document);
                    document.save(destFilePath);
                }
                log.info("Compression FINISHED!! Saved to : {}", destFilePath);
            }
        }

        catch(Exception e){
            log.error("Failed to compress PDF for Task ID: {}", task.getId(),e);
        }
    }


    private void applyMaxCompression(PDDocument document){

        document.setDocumentInformation(new org.apache.pdfbox.pdmodel.PDDocumentInformation());

        if(document.getDocumentCatalog() != null){
            document.getDocumentCatalog().setMetadata(null);
        }


        log.info("Applied MAX Compression: Stripped all metadata and optimized structure.");
    }


    private void applyStandardCompression(PDDocument document){

        if(document.getDocumentCatalog() != null){
            document.getDocumentCatalog().setMetadata(null);
        }
        
        log.info("Applied STANDARD Compression: Maintained metadata, basic compression only.");
    }
}
