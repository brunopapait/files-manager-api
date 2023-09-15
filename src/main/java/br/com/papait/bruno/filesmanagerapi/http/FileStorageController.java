package br.com.papait.bruno.filesmanagerapi.http;

import br.com.papait.bruno.filesmanagerapi.config.FileStoragePropertiesConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/api/files")
public class FileStorageController {
  private final Path fileStoreLocation;

  public FileStorageController(FileStoragePropertiesConfig fileStoragePropertiesConfig) {
    this.fileStoreLocation = Paths.get(fileStoragePropertiesConfig.getUploadDir()).toAbsolutePath().normalize();
  }

  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {
    String name = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
    try {
      Path targetLocation = this.fileStoreLocation.resolve(name);
      file.transferTo(targetLocation);

      String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/api/files/download/")
          .path(name)
          .toUriString();

      return ResponseEntity.ok("Upload completed! Download link:" + fileDownloadUri);
    } catch (IOException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/download/{fileName:.+}")
  public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest httpServletRequest) throws IOException {
    Path filePath = this.fileStoreLocation.resolve(fileName).normalize();

    try {
      Resource resource = new UrlResource(filePath.toUri());
      String contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

      if (Objects.isNull(contentType)) {
        contentType = "application/octet-stream";
      }

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
          .body(resource);
    } catch (MalformedURLException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/list")
  public ResponseEntity<List<String>> listAllFiles() throws IOException {
    List<String> files = Files.list(this.fileStoreLocation)
        .map(Path::getFileName)
        .map(Path::toString)
        .toList();

    return ResponseEntity.ok(files);
  }
}
