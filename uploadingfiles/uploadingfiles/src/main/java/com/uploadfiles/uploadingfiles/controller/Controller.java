package com.uploadfiles.uploadingfiles.controller;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uploadfiles.uploadingfiles.dto.Response;
import com.uploadfiles.uploadingfiles.service.MyService;

@RestController
@RequestMapping("/google")
public class Controller {

	@Autowired
	private MyService service;

	@PostMapping("/upload/{fileName}")
	public Object handleFileUpload(@PathVariable String fileName,@RequestParam("file") MultipartFile file)
			throws IOException, GeneralSecurityException {
		if (file.isEmpty()) {
			return "File is empty";
		}
		File tempFile = File.createTempFile("temp", ".csv");
		file.transferTo(tempFile);
		Response response = service.uploadFileToDrive(tempFile,fileName);
		System.out.println(response);
		return response;
	}

	@GetMapping("/folders/{folderName}/id")
	public String getFolderId(@PathVariable String folderName) throws GeneralSecurityException {
		try {
			String folderId = service.getFolderId(folderName);
			if (folderId != null) {
				return folderId;
			} else {
				return "Folder not found";
			}
		} catch (IOException e) {
			e.printStackTrace(); // Log or handle the exception appropriately
			return "Error occurred while retrieving folder ID";
		}
	}

	@GetMapping("/folders/{folderName}/files")
	public List<com.google.api.services.drive.model.File> getFilesInFolder(@PathVariable String folderName)
			throws GeneralSecurityException {
		try {
			return service.getFilesInFolder(folderName);
		} catch (IOException e) {
			e.printStackTrace(); // Log or handle the exception appropriately
			return Collections.emptyList();
		}
	}

	@GetMapping("/folders/{folderName}/files/{fileName}/exists")
	public boolean isFilePresentInFolder(@PathVariable String folderName, @PathVariable String fileName)
			throws GeneralSecurityException {
		try {
			return service.isFilePresentInFolder(folderName, fileName);
		} catch (IOException e) {
			e.printStackTrace(); // Log or handle the exception appropriately
			return false;
		}
	}
	
	@GetMapping("/folders/{folderName}/files/{fileName}/process")
    public String processFile(@PathVariable String folderName, @PathVariable String fileName) throws GeneralSecurityException, IOException {
        return service.processFile(folderName, fileName);
    }
	
	@GetMapping("/folders/{folderName}/file/{fileName}/exist")
    public com.google.api.services.drive.model.File isFileInFolder(@PathVariable String folderName, @PathVariable String fileName) {
        try {
            return service.isFileInFolder(folderName, fileName);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace(); // Log or handle the exception appropriately
            return null;
        }
    }
	
}
