package com.telus.spring.ai.resume.ui;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.telus.spring.ai.resume.model.chat.ChatRequest;
import com.telus.spring.ai.resume.model.chat.ChatResponse;
import com.telus.spring.ai.resume.service.ResumeAwareChatService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * View for matching a job description against resumes.
 * This view can be accessed via /match-new or /match-new?jd=...
 */
@Route(value = "match-new", layout = MainLayout.class)
@PageTitle("Match Job Description")
@AnonymousAllowed
public class MatchNewView extends VerticalLayout implements HasUrlParameter<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(MatchNewView.class);
    
    private final ResumeAwareChatService chatService;
    
    private TextArea jobDescriptionArea;
    private Button analyzeButton;
    private ProgressBar progressBar;
    private Div resultContent;
    private VerticalLayout resultContainer;
    
    @Autowired
    public MatchNewView(ResumeAwareChatService chatService) {
        this.chatService = chatService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        add(createHeader());
        add(createMainContent());
    }
    
    private VerticalLayout createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(false);
        header.setAlignItems(Alignment.CENTER);
        
        H2 title = new H2("Resume Matcher");
        title.getStyle().set("margin-top", "0");
        
        Paragraph subtitle = new Paragraph("Enter a job description to get AI-powered insights on what makes a good match");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        header.add(title, subtitle);
        return header;
    }
    
    private VerticalLayout createMainContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        // Job description input
        jobDescriptionArea = new TextArea("Job Description");
        jobDescriptionArea.setWidthFull();
        jobDescriptionArea.setMinHeight("200px");
        jobDescriptionArea.setPlaceholder("Paste job description here...");
        
        // Analyze button
        analyzeButton = new Button("Analyze Job Description", new Icon(VaadinIcon.SEARCH));
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.setWidthFull();
        analyzeButton.addClickListener(e -> analyzeJobDescription());
        
        // Progress indicator
        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setWidthFull();
        
        // Results container
        resultContainer = new VerticalLayout();
        resultContainer.setWidthFull();
        resultContainer.setPadding(true);
        resultContainer.setSpacing(true);
        resultContainer.setVisible(false);
        resultContainer.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        resultContainer.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        resultContainer.getStyle().set("border-left", "4px solid var(--lumo-primary-color)");
        
        H3 resultTitle = new H3("Analysis Results");
        resultContent = new Div();
        resultContent.setWidthFull();
        
        resultContainer.add(resultTitle, resultContent);
        
        content.add(jobDescriptionArea, analyzeButton, progressBar, resultContainer);
        return content;
    }
    
    private void analyzeJobDescription() {
        String jobDescription = jobDescriptionArea.getValue().trim();
        
        if (jobDescription.isEmpty()) {
            Notification notification = Notification.show("Please enter a job description");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        analyzeButton.setEnabled(false);
        
        // Create a random user ID for this request
        String userId = UUID.randomUUID().toString();
        
        // Create request
        ChatRequest request = new ChatRequest();
        request.setCurrentJobDescription(jobDescription);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Call the service
                    ChatResponse response = chatService.chat(
                        userId,
                        "Please analyze this job description and tell me what skills and qualifications would make a good match: " + jobDescription,
                        null,
                        jobDescription
                    );
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        analyzeButton.setEnabled(true);
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error analyzing job description", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        analyzeButton.setEnabled(true);
                        
                        Notification notification = Notification.show("Error analyzing job description: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        // Check for job description in URL parameters
        event.getLocation().getQueryParameters().getParameters().getOrDefault("jd", null)
            .stream().findFirst().ifPresent(jd -> {
                jobDescriptionArea.setValue(jd);
                analyzeJobDescription();
            });
    }
}
