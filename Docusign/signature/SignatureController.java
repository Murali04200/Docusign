package com.example.Docusign.signature;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SignatureController {

    private final EditorStore editorStore;

    public SignatureController(EditorStore editorStore) {
        this.editorStore = editorStore;
    }

    @GetMapping("/send-signature")
    public String sendSignaturePage() {
        return "send-signature";
    }

    @PostMapping("/send-signature/next")
    public String sendSignatureNext(
            @RequestParam(name = "document", required = false) MultipartFile document,
            @RequestParam(name = "recipientName", required = false) String[] recipientNames,
            @RequestParam(name = "recipientEmail", required = false) String[] recipientEmails,
            @RequestParam(name = "permission", required = false) String[] permissions,
            RedirectAttributes redirectAttributes
    ) throws IOException {
        if (document == null || document.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please upload a document.");
            return "redirect:/send-signature";
        }

        // Save to temp file
        String original = document.getOriginalFilename();
        String suffix = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".bin";
        Path tmp = Files.createTempFile("envelope-", suffix);
        Files.write(tmp, document.getBytes());

        // Map recipients
        List<EditorStore.Recipient> list = new ArrayList<>();
        if (recipientNames != null && recipientEmails != null && permissions != null) {
            int count = Math.min(recipientNames.length, Math.min(recipientEmails.length, permissions.length));
            for (int i = 0; i < count; i++) {
                EditorStore.Recipient r = new EditorStore.Recipient();
                r.name = recipientNames[i];
                r.email = recipientEmails[i];
                r.permission = permissions[i];
                list.add(r);
            }
        }
        EditorStore.Recipient[] arr = list.toArray(new EditorStore.Recipient[0]);

        String id = editorStore.save(tmp, original != null ? original : "document", document.getContentType(), arr);
        return "redirect:/send-signature/editor?id=" + id;
    }

    @GetMapping("/send-signature/editor")
    public String editorPage(@RequestParam("id") String id, Model model) {
        EditorStore.TempEnvelope env = editorStore.get(id);
        if (env == null) return "redirect:/send-signature";
        model.addAttribute("envId", id);
        model.addAttribute("fileName", env.fileName);
        model.addAttribute("recipients", env.recipients);
        return "send-signature-editor";
    }

    @GetMapping("/send-signature/file")
    public ResponseEntity<FileSystemResource> getFile(@RequestParam("id") String id) {
        EditorStore.TempEnvelope env = editorStore.get(id);
        if (env == null || env.filePath == null) return ResponseEntity.notFound().build();
        FileSystemResource res = new FileSystemResource(env.filePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + env.fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(res);
    }

    @PostMapping("/send-signature/placements")
    public String savePlacements(@RequestParam("id") String id,
                                 @RequestParam("placements") String placements,
                                 RedirectAttributes redirectAttributes) {
        editorStore.setPlacements(id, placements);
        redirectAttributes.addFlashAttribute("message", "Fields saved.");
        return "redirect:/send-signature/editor?id=" + id;
    }

    @PostMapping("/send-signature/send")
    public String sendEnvelope(@RequestParam("id") String id,
                               @RequestParam("placements") String placements,
                               RedirectAttributes redirectAttributes) {
        editorStore.setPlacements(id, placements);
        // TODO: Create and send envelope via provider API using file + recipients + placements
        editorStore.remove(id);
        redirectAttributes.addFlashAttribute("message", "Envelope created and sent successfully.");
        return "redirect:/dashboard";
    }
}
