package com.telus.spring.ai.resume.ui;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeParseResult;
import com.telus.spring.ai.resume.service.ResumeParserService;
import com.telus.spring.ai.resume.service.ResumeStorageService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.InputStream;
import java.util.UUID;

/**
 * The upload view allows users to upload resumes.
 */
@Route(value = "upload", layout = MainLayout.class)
@PageTitle("Resume AI - Upload Resume")
public class UploadView extends VerticalLayout {

    private final ResumeParserService parserService;
    private final ResumeStorageService storageService;
    
    private Upload upload;
    private MemoryBuffer buffer;
    private ProgressBar progressBar;
    private VerticalLayout resultLayout;
    private String originalFileName;

    public UploadView(ResumeParserService parserService, ResumeStorageService storageService) {
        this.parserService = parserService;
        this.storageService = storageService;
        
        addClassName("upload-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        
        add(createHeaderSection());
        add(createUploadSection());
        add(createResultSection());
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout section = new VerticalLayout();
        section.setAlignItems(Alignment.CENTER);
        section.setWidthFull();
        
        H1 title = new H1("Upload Resume");
        title.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        Paragraph description = new Paragraph(
                "Upload a resume in PDF or DOCX format to extract information and add it to the system.");
        description.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        section.add(title, description);
        return section;
    }

    private VerticalLayout createUploadSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        section.setWidthFull();
        section.setMaxWidth("800px");
        
        H3 sectionTitle = new H3("Upload File");
        
        // Create upload component
        buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setWidthFull();
        
        // Add upload start listener
        upload.addStartedListener(event -> {
            originalFileName = event.getFileName();
            progressBar.setVisible(true);
            resultLayout.setVisible(false);
        });
        
        // Add upload success listener
        upload.addSucceededListener(event -> {
            try {
                // Get uploaded file
                InputStream inputStream = buffer.getInputStream();
                String mimeType = event.getMIMEType();
                
                // Create a MultipartFile from the InputStream
                MultipartFile file = new MockMultipartFile(
                        originalFileName,
                        originalFileName,
                        mimeType,
                        inputStream);
                
                // Parse resume
                ResumeParseResult parseResult = parserService.parseResume(file);
                
                // Store resume
              //  Resume resume = storageService.storeResume(parseResult, file);
                
                // Show result
                //showResult(resume);
                
                // Show success notification
                Notification notification = new Notification(
                        "Resume uploaded and parsed successfully", 
                        3000, 
                        Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                notification.open();
                
            } catch (Exception e) {
                // Show error notification
                Notification notification = new Notification(
                        "Error processing resume: " + e.getMessage(), 
                        5000, 
                        Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } finally {
                progressBar.setVisible(false);
            }
        });
        
        // Add upload fail listener
        upload.addFailedListener(event -> {
            progressBar.setVisible(false);
            
            // Show error notification
            Notification notification = new Notification(
                    "Upload failed: " + event.getReason(), 
                    5000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        });
        
        // Progress bar
        progressBar = new ProgressBar();
        progressBar.setWidthFull();
        progressBar.setVisible(false);
        
        section.add(sectionTitle, upload, progressBar);
        return section;
    }

    private VerticalLayout createResultSection() {
        resultLayout = new VerticalLayout();
        resultLayout.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        resultLayout.setWidthFull();
        resultLayout.setMaxWidth("800px");
        resultLayout.setVisible(false);
        
        return resultLayout;
    }

    private void showResult(Resume resume) {
        resultLayout.removeAll();
        
        H3 sectionTitle = new H3("Resume Parsed Successfully");
        
        Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
        icon.setSize("50px");
        icon.setColor("var(--lumo-success-color)");
        
        // Create result info
        VerticalLayout resultInfo = new VerticalLayout();
        resultInfo.setPadding(false);
        resultInfo.setSpacing(true);
        
        // Name
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setWidthFull();
        
        Paragraph nameLabel = new Paragraph("Name:");
        nameLabel.getStyle().set("font-weight", "bold");
        nameLabel.setWidth("150px");
        
        Paragraph nameValue = new Paragraph(resume.getName());
        
        nameLayout.add(nameLabel, nameValue);
        
        // Email
        HorizontalLayout emailLayout = new HorizontalLayout();
        emailLayout.setWidthFull();
        
        Paragraph emailLabel = new Paragraph("Email:");
        emailLabel.getStyle().set("font-weight", "bold");
        emailLabel.setWidth("150px");
        
        Paragraph emailValue = new Paragraph(resume.getEmail());
        
        emailLayout.add(emailLabel, emailValue);
        
        // Phone
        HorizontalLayout phoneLayout = new HorizontalLayout();
        phoneLayout.setWidthFull();
        
        Paragraph phoneLabel = new Paragraph("Phone:");
        phoneLabel.getStyle().set("font-weight", "bold");
        phoneLabel.setWidth("150px");
        
        Paragraph phoneValue = new Paragraph(resume.getPhoneNumber());
        
        phoneLayout.add(phoneLabel, phoneValue);
        
        // File info
        HorizontalLayout fileLayout = new HorizontalLayout();
        fileLayout.setWidthFull();
        
        Paragraph fileLabel = new Paragraph("File:");
        fileLabel.getStyle().set("font-weight", "bold");
        fileLabel.setWidth("150px");
        
        Paragraph fileValue = new Paragraph(resume.getOriginalFileName());
        
        fileLayout.add(fileLabel, fileValue);
        
        resultInfo.add(nameLayout, emailLayout, phoneLayout, fileLayout);
        
        // Action buttons
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.CENTER);
        actions.setSpacing(true);
        actions.setPadding(true);
        
        Button viewButton = new Button("View Resume", new Icon(VaadinIcon.EYE));
        viewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewButton.addClickListener(e -> viewResume(resume.getId()));
        
        Button uploadAnotherButton = new Button("Upload Another", new Icon(VaadinIcon.UPLOAD));
        uploadAnotherButton.addClickListener(e -> {
            upload.getElement().callJsFunction("clear");
            resultLayout.setVisible(false);
        });
        
        Button viewAllButton = new Button("View All Resumes", new Icon(VaadinIcon.LIST));
        viewAllButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("resumes")));
        
        actions.add(viewButton, uploadAnotherButton, viewAllButton);
        
        resultLayout.add(sectionTitle, icon, resultInfo, actions);
        resultLayout.setAlignItems(Alignment.CENTER);
        resultLayout.setVisible(true);
    }

    private void viewResume(UUID id) {
        getUI().ifPresent(ui -> ui.navigate("resume/" + id));
    }
}
