package com.telus.spring.ai.resume.ui;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.chat.ChatMessage;
import com.telus.spring.ai.resume.model.chat.ChatResponse;
import com.telus.spring.ai.resume.repository.ResumeRepository;
import com.telus.spring.ai.resume.service.ResumeAwareChatService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * View for the chat interface.
 */
@Route(value = "chat", layout = MainLayout.class)
@PageTitle("Resume Chat")
public class ChatView extends VerticalLayout {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatView.class);
    
    private final ResumeAwareChatService chatService;
    private final ResumeRepository resumeRepository;
    
    private VerticalLayout chatMessagesLayout;
    private TextField messageField;
    private TextArea jobDescriptionArea;
    private ComboBox<Resume> resumeComboBox;
    private String userId;
    private VerticalLayout commandButtonsLayout;
    private Details helpPanel;
    private Div resumePreviewDiv;
    
    @Autowired
    public ChatView(ResumeAwareChatService chatService, ResumeRepository resumeRepository) {
        this.chatService = chatService;
        this.resumeRepository = resumeRepository;
        
        // Generate a unique user ID for this session
        this.userId = UUID.randomUUID().toString();
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        add(createHeader());
        add(createChatArea());
        add(createInputArea());
    }
    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        // Load chat history
        loadChatHistory();
        
        // Load resumes for the dropdown
        loadResumes();
    }
    
    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        
        H2 title = new H2("Resume Chat Assistant");
        title.addClassNames(LumoUtility.Margin.NONE);
        
        Button clearButton = new Button("Clear Chat", new Icon(VaadinIcon.TRASH));
        clearButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        clearButton.addClickListener(e -> clearChat());
        
        header.add(title, clearButton);
        header.setFlexGrow(1, title);
        
        return header;
    }
    
    private Component createChatArea() {
        VerticalLayout chatArea = new VerticalLayout();
        chatArea.setWidthFull();
        chatArea.setHeightFull();
        chatArea.setPadding(true);
        chatArea.setSpacing(true);
        
        // Context selection area
        HorizontalLayout contextArea = new HorizontalLayout();
        contextArea.setWidthFull();
        contextArea.setPadding(true);
        contextArea.setSpacing(true);
        
        // Resume selection
        VerticalLayout resumeLayout = new VerticalLayout();
        resumeLayout.setPadding(false);
        resumeLayout.setSpacing(false);
        
        H3 resumeHeader = new H3("Select Resume");
        resumeHeader.addClassNames(LumoUtility.Margin.NONE);
        
        resumeComboBox = new ComboBox<>();
        resumeComboBox.setWidthFull();
        resumeComboBox.setItemLabelGenerator(Resume::getName);
        resumeComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                addSystemMessage("Selected resume: " + e.getValue().getName());
                updateResumePreview(e.getValue());
            } else {
                clearResumePreview();
            }
        });
        
        // Resume preview
        resumePreviewDiv = new Div();
        resumePreviewDiv.setWidthFull();
        resumePreviewDiv.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        resumePreviewDiv.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        resumePreviewDiv.getStyle().set("padding", "var(--lumo-space-m)");
        resumePreviewDiv.getStyle().set("margin-top", "var(--lumo-space-m)");
        resumePreviewDiv.getStyle().set("font-size", "var(--lumo-font-size-s)");
        resumePreviewDiv.setVisible(false);
        
        resumeLayout.add(resumeHeader, resumeComboBox, resumePreviewDiv);
        
        // Job description area
        VerticalLayout jobLayout = new VerticalLayout();
        jobLayout.setPadding(false);
        jobLayout.setSpacing(false);
        
        H3 jobHeader = new H3("Job Description");
        jobHeader.addClassNames(LumoUtility.Margin.NONE);
        
        jobDescriptionArea = new TextArea();
        jobDescriptionArea.setWidthFull();
        jobDescriptionArea.setMinHeight("150px");
        jobDescriptionArea.setPlaceholder("Enter job description here...");
        
        // Add refine button
        Button refineButton = new Button("Refine Job Description", new Icon(VaadinIcon.EDIT));
        refineButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        refineButton.getStyle().set("margin-top", "var(--lumo-space-xs)");
        refineButton.addClickListener(e -> {
            if (jobDescriptionArea.getValue().trim().isEmpty()) {
                Notification.show("Please enter a job description first", 3000, Notification.Position.MIDDLE);
                return;
            }
            messageField.setValue("/refine ");
            messageField.focus();
        });
        
        jobLayout.add(jobHeader, jobDescriptionArea, refineButton);
        
        // Command buttons
        commandButtonsLayout = new VerticalLayout();
        commandButtonsLayout.setPadding(true);
        commandButtonsLayout.setSpacing(true);
        commandButtonsLayout.setWidthFull();
        commandButtonsLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        commandButtonsLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        
        H3 commandsHeader = new H3("Quick Commands");
        commandsHeader.addClassNames(LumoUtility.Margin.NONE);
        
        // Create command buttons
        HorizontalLayout commandRow1 = new HorizontalLayout();
        commandRow1.setWidthFull();
        commandRow1.setSpacing(true);
        
        Button explainButton = createCommandButton("Explain Match", VaadinIcon.SEARCH, "/explain");
        Button infoButton = createCommandButton("Resume Info", VaadinIcon.INFO_CIRCLE, "/info");
        Button extractButton = createCommandButton("Extract Data", VaadinIcon.DOWNLOAD, "/extract skills");
        
        commandRow1.add(explainButton, infoButton, extractButton);
        commandRow1.setFlexGrow(1, explainButton);
        commandRow1.setFlexGrow(1, infoButton);
        commandRow1.setFlexGrow(1, extractButton);
        
        HorizontalLayout commandRow2 = new HorizontalLayout();
        commandRow2.setWidthFull();
        commandRow2.setSpacing(true);
        
        Button compareButton = createCommandButton("Compare Resumes", VaadinIcon.SCALE, "/compare");
        Button listButton = createCommandButton("List Resumes", VaadinIcon.LIST, "/list");
        Button helpButton = createCommandButton("Help", VaadinIcon.QUESTION_CIRCLE, "/help");
        
        commandRow2.add(compareButton, listButton, helpButton);
        commandRow2.setFlexGrow(1, compareButton);
        commandRow2.setFlexGrow(1, listButton);
        commandRow2.setFlexGrow(1, helpButton);
        
        commandButtonsLayout.add(commandsHeader, commandRow1, commandRow2);
        
        // Help panel
        helpPanel = createHelpPanel();
        
        contextArea.add(resumeLayout, jobLayout);
        contextArea.setFlexGrow(1, resumeLayout);
        contextArea.setFlexGrow(2, jobLayout);
        
        // Chat messages area
        chatMessagesLayout = new VerticalLayout();
        chatMessagesLayout.setWidthFull();
        chatMessagesLayout.setPadding(true);
        chatMessagesLayout.setSpacing(true);
        chatMessagesLayout.getStyle().set("overflow-y", "auto");
        chatMessagesLayout.getStyle().set("flex-grow", "1");
        chatMessagesLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        chatMessagesLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        
        chatArea.add(contextArea, commandButtonsLayout, helpPanel, chatMessagesLayout);
        
        return chatArea;
    }
    
    private Component createInputArea() {
        HorizontalLayout inputArea = new HorizontalLayout();
        inputArea.setWidthFull();
        inputArea.setPadding(true);
        inputArea.setSpacing(true);
        
        messageField = new TextField();
        messageField.setWidthFull();
        messageField.setPlaceholder("Type a message...");
        messageField.addKeyPressListener(Key.ENTER, e -> sendMessage());
        
        Button sendButton = new Button("Send", new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());
        
        inputArea.add(messageField, sendButton);
        inputArea.setFlexGrow(1, messageField);
        
        return inputArea;
    }
    
    private void sendMessage() {
        String message = messageField.getValue().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message to chat
        addUserMessage(message);
        
        // Clear input field
        messageField.clear();
        messageField.focus();
        
        // Get current context
        UUID resumeId = resumeComboBox.getValue() != null ? resumeComboBox.getValue().getId() : null;
        String jobDescription = jobDescriptionArea.getValue();
        
        // Send message to chat service
        UI ui = getUI().orElse(null);
        if (ui != null) {
            ui.setPollInterval(1000);
            ui.access(() -> {
                // Add typing indicator
                Div typingIndicator = new Div();
                typingIndicator.setText("AI is typing...");
                typingIndicator.getStyle().set("font-style", "italic");
                typingIndicator.getStyle().set("color", "var(--lumo-secondary-text-color)");
                typingIndicator.getStyle().set("padding", "var(--lumo-space-m)");
                chatMessagesLayout.add(typingIndicator);
                
                // Scroll to bottom
                scrollToBottom();
            });
            
            // Process in background
            new Thread(() -> {
                try {
                    ChatResponse response = chatService.chat(userId, message, resumeId, jobDescription);
                    
                    ui.access(() -> {
                        // Remove typing indicator
                        chatMessagesLayout.remove(chatMessagesLayout.getComponentAt(chatMessagesLayout.getComponentCount() - 1));
                        
                        // Add AI response
                        addAssistantMessage(response.getMessage());
                        
                        // Check for additional data
                        if (response.getAdditionalData() != null && response.getAdditionalData().containsKey("refinedJobDescription")) {
                            String refinedJobDescription = (String) response.getAdditionalData().get("refinedJobDescription");
                            jobDescriptionArea.setValue(refinedJobDescription);
                            
                            Notification notification = new Notification("Job description updated", 3000);
                            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            notification.open();
                        }
                        
                        ui.setPollInterval(-1);
                    });
                } catch (Exception e) {
                    logger.error("Error processing chat message", e);
                    
                    ui.access(() -> {
                        // Remove typing indicator
                        chatMessagesLayout.remove(chatMessagesLayout.getComponentAt(chatMessagesLayout.getComponentCount() - 1));
                        
                        // Add error message
                        addErrorMessage("Sorry, an error occurred: " + e.getMessage());
                        
                        ui.setPollInterval(-1);
                    });
                }
            }).start();
        }
    }
    
    private void addUserMessage(String message) {
        HorizontalLayout messageLayout = new HorizontalLayout();
        messageLayout.setWidthFull();
        messageLayout.setPadding(true);
        messageLayout.setSpacing(true);
        
        Div messageDiv = new Div();
        messageDiv.setText(message);
        messageDiv.getStyle().set("background-color", "var(--lumo-primary-color)");
        messageDiv.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        messageDiv.getStyle().set("padding", "var(--lumo-space-m)");
        messageDiv.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        messageDiv.getStyle().set("max-width", "80%");
        messageDiv.getStyle().set("word-wrap", "break-word");
        
        messageLayout.add(messageDiv);
        messageLayout.setJustifyContentMode(JustifyContentMode.END);
        
        chatMessagesLayout.add(messageLayout);
        scrollToBottom();
    }
    
    private void addAssistantMessage(String message) {
        HorizontalLayout messageLayout = new HorizontalLayout();
        messageLayout.setWidthFull();
        messageLayout.setPadding(true);
        messageLayout.setSpacing(true);
        
        Div messageDiv = new Div();
        messageDiv.getElement().setProperty("innerHTML", message.replace("\n", "<br>"));
        messageDiv.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        messageDiv.getStyle().set("color", "var(--lumo-body-text-color)");
        messageDiv.getStyle().set("padding", "var(--lumo-space-m)");
        messageDiv.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        messageDiv.getStyle().set("max-width", "80%");
        messageDiv.getStyle().set("word-wrap", "break-word");
        
        messageLayout.add(messageDiv);
        messageLayout.setJustifyContentMode(JustifyContentMode.START);
        
        chatMessagesLayout.add(messageLayout);
        scrollToBottom();
    }
    
    private void addSystemMessage(String message) {
        Paragraph systemMessage = new Paragraph(message);
        systemMessage.getStyle().set("font-style", "italic");
        systemMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
        systemMessage.getStyle().set("text-align", "center");
        
        chatMessagesLayout.add(systemMessage);
        scrollToBottom();
    }
    
    private void addErrorMessage(String message) {
        Paragraph errorMessage = new Paragraph(message);
        errorMessage.getStyle().set("color", "var(--lumo-error-color)");
        errorMessage.getStyle().set("text-align", "center");
        
        chatMessagesLayout.add(errorMessage);
        scrollToBottom();
    }
    
    private void scrollToBottom() {
        if (getUI().isPresent()) {
            getUI().get().getPage().executeJs(
                "setTimeout(function() {" +
                "  const chatArea = document.querySelector('vaadin-vertical-layout > vaadin-vertical-layout > vaadin-vertical-layout');" +
                "  if (chatArea) chatArea.scrollTop = chatArea.scrollHeight;" +
                "}, 100);"
            );
        }
    }
    
    private void loadChatHistory() {
        try {
            for (ChatMessage message : chatService.getChatHistory(userId)) {
                if ("user".equals(message.getRole())) {
                    addUserMessage(message.getContent());
                } else if ("assistant".equals(message.getRole())) {
                    addAssistantMessage(message.getContent());
                }
            }
            
            if (chatMessagesLayout.getComponentCount() == 0) {
                // Add welcome message
                addAssistantMessage("Hello! I'm your resume assistant. I can help you analyze resumes, match them to job descriptions, and provide insights. How can I help you today?");
            }
        } catch (Exception e) {
            logger.error("Error loading chat history", e);
            addErrorMessage("Error loading chat history: " + e.getMessage());
        }
    }
    
    private void loadResumes() {
        try {
            resumeComboBox.setItems(resumeRepository.findAll());
        } catch (Exception e) {
            logger.error("Error loading resumes", e);
            Notification.show("Error loading resumes: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }
    
    private void clearChat() {
        chatMessagesLayout.removeAll();
        
        // Add welcome message
        addAssistantMessage("Chat history cleared. How can I help you today?");
    }
    
    /**
     * Create a command button.
     * 
     * @param text The button text
     * @param icon The button icon
     * @param command The command to insert
     * @return The button
     */
    private Button createCommandButton(String text, VaadinIcon icon, String command) {
        Button button = new Button(text, new Icon(icon));
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.setWidthFull();
        
        button.addClickListener(e -> {
            if (command.equals("/compare")) {
                // For compare, we need to handle multiple resume selection
                if (resumeComboBox.getValue() == null) {
                    Notification.show("Please select at least one resume first", 3000, Notification.Position.MIDDLE);
                    return;
                }
                
                // Start with the selected resume ID
                messageField.setValue(command + " " + resumeComboBox.getValue().getId() + " ");
                Notification.show("Please add another resume ID to compare", 3000, Notification.Position.MIDDLE);
            } else if (command.equals("/extract skills")) {
                // For extract, we need to specify what to extract
                messageField.setValue("/extract ");
                Notification.show("Please specify what to extract (skills, last_company, experience, education, contact)", 
                                 5000, Notification.Position.MIDDLE);
            } else {
                messageField.setValue(command);
            }
            
            messageField.focus();
        });
        
        return button;
    }
    
    /**
     * Create the help panel.
     * 
     * @return The help panel
     */
    private Details createHelpPanel() {
        Details details = new Details("Command Help", createHelpContent());
        details.setWidthFull();
        details.setOpened(false);
        return details;
    }
    
    /**
     * Create the help content.
     * 
     * @return The help content
     */
    private Component createHelpContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        
        // Explain command
        H4 explainHeader = new H4("/explain");
        Paragraph explainDesc = new Paragraph("Get a detailed explanation of how well a resume matches a job description.");
        Paragraph explainUsage = new Paragraph("Usage: /explain [resumeId]");
        explainUsage.getStyle().set("font-style", "italic");
        
        // Refine command
        H4 refineHeader = new H4("/refine");
        Paragraph refineDesc = new Paragraph("Refine a job description based on specific criteria.");
        Paragraph refineUsage = new Paragraph("Usage: /refine add more emphasis on Java skills");
        refineUsage.getStyle().set("font-style", "italic");
        
        // Compare command
        H4 compareHeader = new H4("/compare");
        Paragraph compareDesc = new Paragraph("Compare multiple resumes against a job description.");
        Paragraph compareUsage = new Paragraph("Usage: /compare [resumeId1] [resumeId2] ...");
        compareUsage.getStyle().set("font-style", "italic");
        
        // Info command
        H4 infoHeader = new H4("/info");
        Paragraph infoDesc = new Paragraph("Get detailed information about a specific resume.");
        Paragraph infoUsage = new Paragraph("Usage: /info [resumeId]");
        infoUsage.getStyle().set("font-style", "italic");
        
        // List command
        H4 listHeader = new H4("/list");
        Paragraph listDesc = new Paragraph("Get a list of resumes, optionally filtered by criteria.");
        Paragraph listUsage = new Paragraph("Usage: /list [criteria]");
        listUsage.getStyle().set("font-style", "italic");
        
        // Extract command
        H4 extractHeader = new H4("/extract");
        Paragraph extractDesc = new Paragraph("Extract specific information from a resume.");
        Paragraph extractUsage = new Paragraph("Usage: /extract [field] [resumeId]");
        Paragraph extractFields = new Paragraph("Available fields: last_company, experience, education, skills, contact");
        extractUsage.getStyle().set("font-style", "italic");
        extractFields.getStyle().set("font-style", "italic");
        
        // Help command
        H4 helpHeader = new H4("/help");
        Paragraph helpDesc = new Paragraph("Display this help information.");
        Paragraph helpUsage = new Paragraph("Usage: /help");
        helpUsage.getStyle().set("font-style", "italic");
        
        content.add(
            explainHeader, explainDesc, explainUsage,
            refineHeader, refineDesc, refineUsage,
            compareHeader, compareDesc, compareUsage,
            infoHeader, infoDesc, infoUsage,
            listHeader, listDesc, listUsage,
            extractHeader, extractDesc, extractUsage, extractFields,
            helpHeader, helpDesc, helpUsage
        );
        
        return content;
    }
    
    /**
     * Update the resume preview.
     * 
     * @param resume The resume to preview
     */
    private void updateResumePreview(Resume resume) {
        if (resume == null) {
            clearResumePreview();
            return;
        }
        
        StringBuilder preview = new StringBuilder();
        preview.append("<strong>").append(resume.getName()).append("</strong><br>");
        preview.append("<small>").append(resume.getEmail()).append(" | ").append(resume.getPhoneNumber()).append("</small><br>");
        preview.append("<small>ID: ").append(resume.getId()).append("</small>");
        
        resumePreviewDiv.getElement().setProperty("innerHTML", preview.toString());
        resumePreviewDiv.setVisible(true);
    }
    
    /**
     * Clear the resume preview.
     */
    private void clearResumePreview() {
        resumePreviewDiv.getElement().setProperty("innerHTML", "");
        resumePreviewDiv.setVisible(false);
    }
}
