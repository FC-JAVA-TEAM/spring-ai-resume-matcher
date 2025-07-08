package com.telus.spring.ai.resume.ui;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.model.chat.ChatResponse;
import com.telus.spring.ai.resume.repository.ResumeRepository;
import com.telus.spring.ai.resume.service.ResumeAwareChatService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
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
    private final ResumeRepository resumeRepository;
    
    private TextArea jobDescriptionArea;
    private ProgressBar progressBar;
    private Div resultContent;
    private VerticalLayout resultContainer;
    private CheckboxGroup<Resume> resumeCheckboxGroup;
    private Select<String> actionSelect;
    private Button executeButton;
    private String userId;
    private com.vaadin.flow.component.checkbox.Checkbox excludeLockedCheckbox;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Available actions
    private static final String ACTION_ANALYZE = "Analyze Job Description";
    private static final String ACTION_MATCH = "Match with Selected Resumes";
    private static final String ACTION_COMPARE = "Compare Selected Resumes";
    private static final String ACTION_EXTRACT_SKILLS = "Extract Skills from Resumes";
    private static final String ACTION_GET_INFO = "Get Detailed Info for Resumes";
    private static final String ACTION_REFINE = "Refine Job Description";
    
    @Autowired
    public MatchNewView(ResumeAwareChatService chatService, ResumeRepository resumeRepository) {
        this.chatService = chatService;
        this.resumeRepository = resumeRepository;
        
        // Generate a unique user ID for this session
        this.userId = UUID.randomUUID().toString();
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        add(createHeader());
        add(createMainContent());
        
        // Load resumes
        loadResumes();
    }
    
    /**
     * Load resumes from the repository.
     */
    private void loadResumes() {
        try {
            resumeCheckboxGroup.setItems(resumeRepository.findAll());
        } catch (Exception e) {
            logger.error("Error loading resumes", e);
            Notification.show("Error loading resumes: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }
    
    private VerticalLayout createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(false);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        
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
        
        // Resume selection with checkboxes
        VerticalLayout resumeSelectionLayout = new VerticalLayout();
        resumeSelectionLayout.setWidthFull();
        resumeSelectionLayout.setPadding(false);
        resumeSelectionLayout.setSpacing(true);
        
        // Add checkbox for excluding locked candidates
        excludeLockedCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Exclude locked candidates");
        excludeLockedCheckbox.setValue(true);
        excludeLockedCheckbox.getStyle().set("margin-top", "10px");
        
        // Header with count of selected resumes
        HorizontalLayout resumeHeaderLayout = new HorizontalLayout();
        resumeHeaderLayout.setWidthFull();
        resumeHeaderLayout.setSpacing(true);
        
        Span resumeLabel = new Span("Select Resumes");
        Span selectedCountLabel = new Span("(0 selected)");
        selectedCountLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Button selectAllButton = new Button("Select All");
        selectAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        
        Button clearAllButton = new Button("Clear");
        clearAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        
        resumeHeaderLayout.add(resumeLabel, selectedCountLabel, selectAllButton, clearAllButton);
        
        // Checkbox group for resumes
        resumeCheckboxGroup = new CheckboxGroup<>();
        resumeCheckboxGroup.setWidthFull();
        resumeCheckboxGroup.setItemLabelGenerator(Resume::getName);
        
        // Update selected count when selection changes
        resumeCheckboxGroup.addValueChangeListener(e -> {
            int selectedCount = e.getValue().size();
            selectedCountLabel.setText("(" + selectedCount + " selected)");
            updateExecuteButtonState();
        });
        
        // Select all and clear buttons
        selectAllButton.addClickListener(e -> {
            Set<Resume> allResumes = resumeRepository.findAll().stream().collect(Collectors.toSet());
            resumeCheckboxGroup.setValue(allResumes);
        });
        
        clearAllButton.addClickListener(e -> resumeCheckboxGroup.clear());
        
        resumeSelectionLayout.add(resumeHeaderLayout, resumeCheckboxGroup);
        
        // Action selection
        VerticalLayout actionSelectionLayout = new VerticalLayout();
        actionSelectionLayout.setWidthFull();
        actionSelectionLayout.setPadding(false);
        actionSelectionLayout.setSpacing(true);
        
        // Action dropdown
        actionSelect = new Select<>();
        actionSelect.setLabel("Select Action");
        actionSelect.setWidthFull();
        actionSelect.setItems(
            ACTION_ANALYZE,
            ACTION_MATCH,
            ACTION_COMPARE,
            ACTION_EXTRACT_SKILLS,
            ACTION_GET_INFO,
            ACTION_REFINE
        );
        actionSelect.setValue(ACTION_ANALYZE);
        
        // Execute button
        executeButton = new Button("Execute", new Icon(VaadinIcon.PLAY));
        executeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        executeButton.setWidthFull();
        executeButton.addClickListener(e -> executeSelectedAction());
        
        // Update button state when job description changes
        jobDescriptionArea.addValueChangeListener(e -> updateExecuteButtonState());
        
        // Update button state when action changes
        actionSelect.addValueChangeListener(e -> updateExecuteButtonState());
        
        actionSelectionLayout.add(actionSelect, excludeLockedCheckbox, executeButton);
        
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
        
        content.add(jobDescriptionArea, resumeSelectionLayout, actionSelectionLayout, progressBar, resultContainer);
        return content;
    }
    
    /**
     * Update the state of the execute button based on the current selections.
     */
    private void updateExecuteButtonState() {
        String selectedAction = actionSelect.getValue();
        boolean hasJobDescription = !jobDescriptionArea.getValue().trim().isEmpty();
        Set<Resume> selectedResumes = resumeCheckboxGroup.getValue();
        
        if (ACTION_ANALYZE.equals(selectedAction)) {
            // For analyze, we only need a job description
            executeButton.setEnabled(hasJobDescription);
        } else if (ACTION_REFINE.equals(selectedAction)) {
            // For refine, we only need a job description
            executeButton.setEnabled(hasJobDescription);
        } else if (ACTION_COMPARE.equals(selectedAction)) {
            // For compare, we need at least 2 resumes and a job description
            executeButton.setEnabled(hasJobDescription && selectedResumes.size() >= 2);
        } else {
            // For all other actions, we need at least 1 resume
            executeButton.setEnabled(hasJobDescription && !selectedResumes.isEmpty());
        }
    }
    
    /**
     * Execute the selected action.
     */
    private void executeSelectedAction() {
        String selectedAction = actionSelect.getValue();
        
        switch (selectedAction) {
            case ACTION_ANALYZE:
                analyzeJobDescription();
                break;
            case ACTION_MATCH:
                matchWithResumes();
                break;
            case ACTION_COMPARE:
                compareResumes();
                break;
            case ACTION_EXTRACT_SKILLS:
                extractSkills();
                break;
            case ACTION_GET_INFO:
                getResumeInfo();
                break;
            case ACTION_REFINE:
                refineJobDescription();
                break;
            default:
                Notification.show("Unknown action: " + selectedAction);
        }
    }
    
    /**
     * Analyze the job description.
     */
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
        executeButton.setEnabled(false);
        
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
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error analyzing job description", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error analyzing job description: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Match the job description with selected resumes.
     * This method uses the new endpoint in CandidateStatusController that supports excluding locked candidates.
     */
    private void matchWithResumes() {
        String jobDescription = jobDescriptionArea.getValue().trim();
        Set<Resume> selectedResumes = resumeCheckboxGroup.getValue();
        boolean excludeLocked = excludeLockedCheckbox.getValue();
        
        if (jobDescription.isEmpty()) {
            Notification notification = Notification.show("Please enter a job description");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (selectedResumes.isEmpty()) {
            Notification notification = Notification.show("Please select at least one resume");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // If only one resume is selected, use single resume match
        if (selectedResumes.size() == 1) {
            matchWithSingleResume(selectedResumes.iterator().next(), jobDescription);
            return;
        }
        
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        executeButton.setEnabled(false);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Call the new endpoint in CandidateStatusController
                    String url = "/api/candidate-status/match";
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    
                    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                    map.add("jd", jobDescription);
                    map.add("limit", "10");
                    map.add("excludeLocked", String.valueOf(excludeLocked));
                    
                    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
                    
                    ResponseEntity<List<ResumeMatch>> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<List<ResumeMatch>>() {}
                    );
                    
                    List<ResumeMatch> matches = response.getBody();
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the results
                        StringBuilder resultHtml = new StringBuilder();
                        resultHtml.append("<h3>Match Results</h3>");
                        
                        if (matches == null || matches.isEmpty()) {
                            resultHtml.append("<p>No matching resumes found.</p>");
                        } else {
                            resultHtml.append("<p>Found ").append(matches.size()).append(" matching resumes:</p>");
                            resultHtml.append("<ul>");
                            
                            for (ResumeMatch match : matches) {
                                Resume resume = match.getResume();
                                resultHtml.append("<li>");
                                resultHtml.append("<strong>").append(resume.getName()).append("</strong>");
                                resultHtml.append(" (Score: ").append(match.getScore()).append("/100)");
                                
                                if (match.isLocked()) {
                                    resultHtml.append(" <span style='color: red;'>[LOCKED]</span>");
                                }
                                
                                resultHtml.append("<br>");
                                resultHtml.append("<em>").append(resume.getEmail()).append("</em>");
                                
                                if (match.getExplanation() != null) {
                                    resultHtml.append("<p>").append(match.getExplanation().replace("\n", "<br>")).append("</p>");
                                }
                                
                                resultHtml.append("</li>");
                            }
                            
                            resultHtml.append("</ul>");
                        }
                        
                        resultContent.getElement().setProperty("innerHTML", resultHtml.toString());
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error matching resumes", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error matching resumes: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Match a single resume with the job description.
     */
    private void matchWithSingleResume(Resume resume, String jobDescription) {
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        executeButton.setEnabled(false);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Call the service
                    ChatResponse response = chatService.chat(
                        userId,
                        "I have selected the resume for " + resume.getName() + 
                        ". Please analyze how well this resume matches the job description and provide detailed feedback: " + 
                        jobDescription,
                        resume.getId(), // Pass the single resume ID
                        jobDescription
                    );
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error matching resume", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error matching resume: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Compare multiple resumes against a job description.
     */
    private void compareResumes() {
        String jobDescription = jobDescriptionArea.getValue().trim();
        Set<Resume> selectedResumes = resumeCheckboxGroup.getValue();
        
        if (jobDescription.isEmpty()) {
            Notification notification = Notification.show("Please enter a job description");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (selectedResumes.size() < 2) {
            Notification notification = Notification.show("Please select at least two resumes to compare");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        executeButton.setEnabled(false);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Build a list of resume names for the prompt
                    List<String> resumeNames = selectedResumes.stream()
                        .map(Resume::getName)
                        .collect(Collectors.toList());
                    
                    // Call the service
                    ChatResponse response = chatService.chat(
                        userId,
                        "I have selected these resumes: " + String.join(", ", resumeNames) + 
                        ". Please compare them against each other for this job description. " +
                        "Highlight the strengths and weaknesses of each candidate compared to the others: " + 
                        jobDescription,
                        null, // We can't pass multiple resume IDs directly, so we'll include them in the message
                        jobDescription
                    );
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error comparing resumes", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error comparing resumes: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Extract skills from selected resumes.
     */
    private void extractSkills() {
        String jobDescription = jobDescriptionArea.getValue().trim();
        Set<Resume> selectedResumes = resumeCheckboxGroup.getValue();
        
        if (jobDescription.isEmpty()) {
            Notification notification = Notification.show("Please enter a job description");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (selectedResumes.isEmpty()) {
            Notification notification = Notification.show("Please select at least one resume");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        executeButton.setEnabled(false);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Build a list of resume names for the prompt
                    List<String> resumeNames = selectedResumes.stream()
                        .map(Resume::getName)
                        .collect(Collectors.toList());
                    
                    // Call the service
                    ChatResponse response = chatService.chat(
                        userId,
                        "I have selected these resumes: " + String.join(", ", resumeNames) + 
                        ". Please extract all skills from these resumes and categorize them. " +
                        "Also highlight which skills are most relevant to this job description: " + 
                        jobDescription,
                        null, // We can't pass multiple resume IDs directly, so we'll include them in the message
                        jobDescription
                    );
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error extracting skills", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error extracting skills: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Get detailed information about selected resumes.
     */
    private void getResumeInfo() {
        String jobDescription = jobDescriptionArea.getValue().trim();
        Set<Resume> selectedResumes = resumeCheckboxGroup.getValue();
        
        if (jobDescription.isEmpty()) {
            Notification notification = Notification.show("Please enter a job description");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (selectedResumes.isEmpty()) {
            Notification notification = Notification.show("Please select at least one resume");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        executeButton.setEnabled(false);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Build a list of resume names for the prompt
                    List<String> resumeNames = selectedResumes.stream()
                        .map(Resume::getName)
                        .collect(Collectors.toList());
                    
                    // Call the service
                    ChatResponse response = chatService.chat(
                        userId,
                        "I have selected these resumes: " + String.join(", ", resumeNames) + 
                        ". Please provide detailed information about each candidate including their " +
                        "education, experience, skills, and other relevant information. " +
                        "Also suggest some good interview questions based on their background and this job description: " + 
                        jobDescription,
                        null, // We can't pass multiple resume IDs directly, so we'll include them in the message
                        jobDescription
                    );
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                    });
                } catch (Exception e) {
                    logger.error("Error getting resume info", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error getting resume info: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Show a dialog to get refinement criteria for the job description.
     */
    private void refineJobDescription() {
        String jobDescription = jobDescriptionArea.getValue().trim();
        
        if (jobDescription.isEmpty()) {
            Notification notification = Notification.show("Please enter a job description");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Create dialog
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        
        H3 title = new H3("Refine Job Description");
        
        TextArea refinementArea = new TextArea("Refinement Criteria");
        refinementArea.setWidthFull();
        refinementArea.setMinHeight("150px");
        refinementArea.setPlaceholder("Enter criteria to refine the job description, e.g., 'add more emphasis on leadership skills', 'focus more on technical requirements', etc.");
        
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        Button refineButton = new Button("Refine", e -> {
            String refinementCriteria = refinementArea.getValue().trim();
            if (refinementCriteria.isEmpty()) {
                Notification.show("Please enter refinement criteria");
                return;
            }
            
            dialog.close();
            processRefinement(jobDescription, refinementCriteria);
        });
        refineButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        buttonLayout.add(cancelButton, refineButton);
        dialogLayout.add(title, refinementArea, buttonLayout);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    /**
     * Process the refinement of the job description.
     * 
     * @param jobDescription The original job description
     * @param refinementCriteria The criteria for refinement
     */
    private void processRefinement(String jobDescription, String refinementCriteria) {
        // Show progress indicator
        progressBar.setVisible(true);
        resultContainer.setVisible(false);
        executeButton.setEnabled(false);
        
        // Process in background
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Call the service
                    ChatResponse response = chatService.chat(
                        userId,
                        "/refine " + refinementCriteria,
                        null,
                        jobDescription
                    );
                    
                    // Update UI with results
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        // Format and display the message
                        String formattedMessage = response.getMessage().replace("\n", "<br>");
                        resultContent.getElement().setProperty("innerHTML", formattedMessage);
                        resultContainer.setVisible(true);
                        
                        // Update the job description if refinement was successful
                        if (response.getAdditionalData() != null && response.getAdditionalData().containsKey("refinedJobDescription")) {
                            String refinedJobDescription = (String) response.getAdditionalData().get("refinedJobDescription");
                            jobDescriptionArea.setValue(refinedJobDescription);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error refining job description", e);
                    
                    ui.access(() -> {
                        progressBar.setVisible(false);
                        executeButton.setEnabled(true);
                        updateExecuteButtonState();
                        
                        Notification notification = Notification.show("Error refining job description: " + e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
        });
    }
    
    /**
     * Set the parameter from the URL.
     * This is required by the HasUrlParameter interface.
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            // Check if the parameter is a job description
            if (parameter.startsWith("jd=")) {
                String jobDescription = parameter.substring(3);
                jobDescriptionArea.setValue(jobDescription);
            }
        }
    }
}
