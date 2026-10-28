package com.example.uploadingfiles;

import com.example.uploadingfiles.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponents;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileUploadController {

    private final StorageService storageService;
    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    //GET /: Looks up the current list of uploaded files from the StorageService and loads it into a Thymeleaf template. It calculates a link to the actual resource by using MvcUriComponentsBuilder.
    //
    //GET /files/{filename}: Loads the resource (if it exists) and sends it to the browser to download by using a Content-Disposition response header.
    //
    //POST /: Handles a multipart message file and gives it to the StorageService for saving.

    @GetMapping("/")
    @ResponseBody
    public String listUploadedFiles(Model model) {

        Function<Path, String> getFileUrlFunction = path ->
                MvcUriComponentsBuilder
                        .fromMethodName(
                                FileUploadController.class,
                                "serveFile",
                                path.getFileName().toString()
                        )
                        .build()
                        .toUri()
                        .toString();
        List<String> files = storageService.loadAll().map(getFileUrlFunction).collect(Collectors.toList());
        model.addAttribute("files", files);
        return "uploadForm";
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);

        if (file == null)
            return ResponseEntity.notFound().build();

        // Set the Content-Disposition header to indicate that the file should be downloaded as an attachment
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        storageService.store(file);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }




}
