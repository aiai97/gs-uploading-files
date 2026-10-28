package com.example.uploadingfiles.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {

  private final Path rootLocation;

  @Autowired
  public FileSystemStorageService(StorageProperties properties) {

        if(properties.getLocation().trim().length() == 0){
            throw new StorageException("File upload location can not be Empty."); 
        }

    this.rootLocation = Paths.get(properties.getLocation());
  }

  @Override
  public void store(MultipartFile file) {
    try {
      if (file.isEmpty()) {
        throw new StorageException("Failed to store empty file " + file.getOriginalFilename());
      }

      //getInputStream()	Get an input stream 	Read the contents of an uploaded file
      //resolve()	Resolve a path	   Combine the directory path with the target file path
      //Files.copy()	Copy a file or stream	  Copy the contents to the target location
      Files.copy(file.getInputStream(), this.rootLocation.resolve(file.getOriginalFilename()));
    } catch (IOException e) {
      throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
    }
  }

  @Override
  public Stream<Path> loadAll() {
    try {
      return Files.walk(this.rootLocation, 1) // walk to the root location with depth 1
          .filter(path -> !path.equals(this.rootLocation)) // filter out the root location itself
          .map(path -> this.rootLocation.relativize(path));  // transform the absolute path to relative path
    } catch (IOException e) {
      throw new StorageException("Failed to read stored files", e);
    }

  }

  @Override
  public Path load(String filename) {
    return rootLocation.resolve(filename);
  }

  // Resource provides a unified way to access resources, such as local files.
  // MalformedURLException indicates a problem with the URL format,
  // while StorageFileNotFoundException is a custom exception used by the application to indicate that a file resource is unavailable.
  @Override
  public Resource loadAsResource(String filename) {
    // in real world, write something like check whether the file can be checked by the user
    // or not, if not, throw a custom exception like AccessDeniedException
    try {
      Path file = load(filename);
      Resource resource = new UrlResource(file.toUri()); // transform the file path to a URL and create a Resource object
      if(resource.exists() || resource.isReadable()) { // check whether the resource exists and is readable
        return resource;
      }
      else {
        throw new StorageFileNotFoundException("Could not read file: " + filename);

      }
    } catch (MalformedURLException e) {
      throw new StorageFileNotFoundException("Could not read file: " + filename, e);
    }
  }

  @Override
  public void deleteAll() {
    FileSystemUtils.deleteRecursively(rootLocation.toFile());
  }

  @Override
  public void init() {
    try {
      Files.createDirectory(rootLocation);
    } catch (IOException e) {
      throw new StorageException("Could not initialize storage", e);
    }
  }
}
