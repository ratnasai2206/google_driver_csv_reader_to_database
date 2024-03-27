package com.uploadfiles.uploadingfiles.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.uploadfiles.uploadingfiles.dto.Response;
import com.uploadfiles.uploadingfiles.entity.User;
import com.uploadfiles.uploadingfiles.repository.UserRepository;

@Service
public class MyService {

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String SERVICE_ACCOUNT_KEY_PATH = getPathToGoogleCredentials();
	
	@Autowired
	private UserRepository repository;

	private static String getPathToGoogleCredentials() {
		String currentDirectory = System.getProperty("user.dir");
		Path filePath = Paths.get(currentDirectory, "credentials.json");
		return filePath.toString();
	}

	public Response uploadFileToDrive(java.io.File file, String folderName) throws GeneralSecurityException, IOException {
		Response response = new Response();
		try {
			String folderId = getFolderId(folderName);
			Drive drive = createDriveService();
			File fileMetaData = new com.google.api.services.drive.model.File();
			fileMetaData.setName(file.getName());
			fileMetaData.setParents(Collections.singletonList(folderId));
			FileContent fileContent = new FileContent("text/csv", file);
			com.google.api.services.drive.model.File uploadFile = drive.files().create(fileMetaData, fileContent)
					.setFields("id").execute();
			String fileUrl = "http://drive.google.com/uc?expect=view&id=" + uploadFile.getId();
			System.out.println("File Url : " + fileUrl);
			file.delete();
			response.setStatus(200);
			response.setMessage("File Successfully Uploaded To Drive");
			response.setUrl(fileUrl);

		} catch (Exception e) {

			System.out.println(e.getMessage());
			response.setStatus(500);
			response.setMessage(e.getMessage());
		}
		return response;
	}

	private Drive createDriveService() throws GeneralSecurityException, IOException {
		GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(SERVICE_ACCOUNT_KEY_PATH))
				.createScoped(Collections.singleton(DriveScopes.DRIVE));
		return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential).build();
	}

	public String getFolderId(String folderName) throws IOException, GeneralSecurityException {
		Drive drive = createDriveService();
		String query = String.format("mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false",
				folderName);
		FileList result = drive.files().list().setQ(query).setFields("files(id)").execute();

		if (!result.getFiles().isEmpty()) {
			return result.getFiles().get(0).getId(); // Assuming there's only one folder with the given name
		} else {
			return null; // Folder not found
		}
	}

	public List<com.google.api.services.drive.model.File> getFilesInFolder(String folderName)
			throws IOException, GeneralSecurityException {
		// Step 1: Obtain the folder ID corresponding to the folder name
		String folderId = getFolderId(folderName);

		// Step 2: Use the obtained folder ID to retrieve the files within that folder
		if (folderId != null) {
			Drive drive = createDriveService();
			String query = String.format("'%s' in parents and trashed = false", folderId);
			FileList result = drive.files().list().setQ(query).execute();
			return result.getFiles();
		} else {
			// Folder not found
			return Collections.emptyList();
		}
	}

	public boolean isFilePresentInFolder(String folderName, String fileName)
			throws IOException, GeneralSecurityException {
		String folderId = getFolderId(folderName);
		Drive drive = createDriveService();
		String query = String.format("'%s' in parents and name = '%s' and trashed = false", folderId, fileName);
		FileList result = drive.files().list().setQ(query).execute();
		return !result.getFiles().isEmpty();
	}

	public File isFileInFolder(String folderName, String fileName)
			throws IOException, GeneralSecurityException {
		String folderId = getFolderId(folderName);
		Drive drive = createDriveService();
		String query = String.format("'%s' in parents and name = '%s' and trashed = false", folderId, fileName);
		FileList result = drive.files().list().setQ(query).execute();
		List<com.google.api.services.drive.model.File> files = result.getFiles();
		return files.get(0);
	}

	public List<User> getFileData(String folderId, String fileName) throws IOException, GeneralSecurityException {
		
		Drive drive = createDriveService();
		String fileId = getFileIdInFolder(folderId, fileName);

		if (fileId != null) {
			// Retrieve file content
			InputStream inputStream = drive.files().get(fileId).executeMediaAsInputStream();
			return csvToUsers(inputStream);
		}
		return null;
	}
	
	private List<User> csvToUsers(InputStream inputStream) {
		List<User> users=new ArrayList<User>();
		try (BufferedReader file=new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
				CSVParser csvParser=new CSVParser(file, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())
				){
			List<CSVRecord> records = csvParser.getRecords();
			for (CSVRecord csvRecord : records) {
				User user=new User(Integer.parseInt(csvRecord.get("id")),csvRecord.get("name"),csvRecord.get("email"),csvRecord.get("password"),Long.parseLong(csvRecord.get("phone")));
				users.add(user);
			}
			return users;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	public String getFileIdInFolder(String folderId, String fileName) throws IOException, GeneralSecurityException {
		Drive drive = createDriveService();
		String query = String.format("'%s' in parents and name = '%s' and trashed = false", folderId, fileName);
		FileList result = drive.files().list().setQ(query).execute();
		if (!result.getFiles().isEmpty()) {
			return result.getFiles().get(0).getId(); // Assuming there's only one file with the given name in the folder
		} else {
			return null; // File not found in the folder
		}
	}
	
	public List<User> storeDataInDatabase(List<User> fileData) {
		// Assuming you have a DAO or repository class to interact with your database
		// Iterate through the fileData list and insert each line into the database
		List<User> saveAll = repository.saveAll(fileData);
		// Optionally, you can commit the transaction or perform any necessary cleanup
		return saveAll;
	}

	public String processFile(String folderName, String fileName) throws IOException, GeneralSecurityException {
		String folderId = getFolderId(folderName);
		System.out.println(folderId);
		// Check if the file is present in the folder
		if(this.isFilePresentInFolder(folderName, fileName)&& this.isFileInFolder(folderName, fileName).getMimeType().equalsIgnoreCase("text/csv")) {
			// Retrieve data from the file
			List<User> fileData = this.getFileData(folderId, fileName);
			storeDataInDatabase(fileData);
			return "Data stored into the database ";
		}
		return "Data not stored into the database ";
		
	}
}
