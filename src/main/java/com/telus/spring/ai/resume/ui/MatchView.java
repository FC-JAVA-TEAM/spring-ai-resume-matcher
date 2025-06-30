package com.telus.spring.ai.resume.ui;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.service.ResumeMatchingService;
import com.telus.spring.ai.resume.service.ResumeStorageService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The match view allows users to match resumes with job descriptions.
 */
@Route(value = "match", layout = MainLayout.class)
@PageTitle("Resume AI - Match Resumes")
public class MatchView extends VerticalLayout {

    private final ResumeMatchingService resumeMatchingService;
    private final ResumeStorageService resumeStorageService;
    
    private TextArea jobDescriptionArea;
    private Button matchButton;
    private ProgressBar progressBar;
    private VerticalLayout resultsLayout;
    private Grid<ResumeMatch> resultsGrid;

    public MatchView(ResumeMatchingService resumeMatchingService, ResumeStorageService resumeStorageService) {
        this.resumeMatchingService = resumeMatchingService;
        this.resumeStorageService = resumeStorageService;
        
        addClassName("match-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        
        add(createHeaderSection());
        add(createInputSection());
        add(createResultsSection());
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout section = new VerticalLayout();
        section.setAlignItems(Alignment.CENTER);
        section.setWidthFull();
        
        H1 title = new H1("Match Resumes");
        title.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        Paragraph description = new Paragraph(
                "Enter a job description to find the best matching resumes in the system.");
        description.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        section.add(title, description);
        return section;
    }

    private VerticalLayout createInputSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        section.setWidthFull();
        section.setMaxWidth("800px");
        
        H3 sectionTitle = new H3("Job Description");
        
        // Job description text area
        jobDescriptionArea = new TextArea();
        jobDescriptionArea.setWidthFull();
        jobDescriptionArea.setMinHeight("200px");
        jobDescriptionArea.setPlaceholder("Enter the job description here...");
        jobDescriptionArea.setHelperText("Provide a detailed job description including required skills, experience, and qualifications.");
        
        // Progress bar
        progressBar = new ProgressBar();
        progressBar.setWidthFull();
        progressBar.setVisible(false);
        
        // Match button
        matchButton = new Button("Find Matching Resumes", new Icon(VaadinIcon.SEARCH));
        matchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        matchButton.setWidthFull();
        matchButton.addClickListener(e -> findMatches());
        
        section.add(sectionTitle, jobDescriptionArea, progressBar, matchButton);
        return section;
    }

    private VerticalLayout createResultsSection() {
        resultsLayout = new VerticalLayout();
        resultsLayout.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        resultsLayout.setWidthFull();
        resultsLayout.setMaxWidth("1000px");
        resultsLayout.setVisible(false);
        
        H3 sectionTitle = new H3("Matching Results");
        
        // Create a horizontal layout for filters/sorting
        HorizontalLayout filtersLayout = new HorizontalLayout();
        filtersLayout.setWidthFull();
        filtersLayout.setJustifyContentMode(JustifyContentMode.END);
        
        // Add a sort by score button
        Button sortButton = new Button("Sort by Score", new Icon(VaadinIcon.SORT));
        sortButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        sortButton.addClickListener(e -> sortResults());
        
        filtersLayout.add(sortButton);
        
        // Create a container for resume cards
        VerticalLayout cardsContainer = new VerticalLayout();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        cardsContainer.setWidthFull();
        
        // We'll populate this container in the showResults method
        
        // Store the cards container as a class field so we can access it later
        resultsGrid = null; // We're not using a grid anymore
        
        resultsLayout.add(sectionTitle, filtersLayout, cardsContainer);
        return resultsLayout;
    }
    
    /**
     * Creates a card component for a resume match.
     */
    private VerticalLayout createResumeMatchCard(ResumeMatch match) {
        // Main card container
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM);
        card.setSpacing(false);
        card.setWidthFull();
        
        // Header section with name and contact info
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        // Name and contact info with icon
        VerticalLayout contactInfo = new VerticalLayout();
        contactInfo.setSpacing(false);
        contactInfo.setPadding(false);
        
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setSpacing(true);
        nameLayout.setPadding(false);
        nameLayout.setAlignItems(Alignment.CENTER);
        
        Icon personIcon = new Icon(VaadinIcon.USER);
        personIcon.setColor("var(--lumo-primary-color)");
        personIcon.setSize("1.2em");
        
        H3 name = new H3(match.getResume().getName());
        name.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.LARGE);
        
        nameLayout.add(personIcon, name);
        
        HorizontalLayout contactDetailsLayout = new HorizontalLayout();
        contactDetailsLayout.setSpacing(true);
        contactDetailsLayout.setPadding(false);
        contactDetailsLayout.setAlignItems(Alignment.CENTER);
        
        Icon emailIcon = new Icon(VaadinIcon.ENVELOPE);
        emailIcon.setColor("var(--lumo-contrast-70pct)");
        emailIcon.setSize("0.9em");
        
        Span email = new Span(match.getResume().getEmail());
        email.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        
        Icon phoneIcon = new Icon(VaadinIcon.PHONE);
        phoneIcon.setColor("var(--lumo-contrast-70pct)");
        phoneIcon.setSize("0.9em");
        phoneIcon.getStyle().set("margin-left", "1em");
        
        Span phone = new Span(match.getResume().getPhoneNumber());
        phone.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        
        contactDetailsLayout.add(emailIcon, email, phoneIcon, phone);
        
        contactInfo.add(nameLayout, contactDetailsLayout);
        
        // Match score section with enhanced visualization
        VerticalLayout scoreSection = new VerticalLayout();
        scoreSection.setSpacing(false);
        scoreSection.setPadding(false);
        scoreSection.setAlignItems(Alignment.END);
        
        // Format score as percentage
        int scorePercent = match.getScore() != null ? match.getScore() : 0;
        
        // Score badge with icon
        HorizontalLayout scoreBadge = new HorizontalLayout();
        scoreBadge.setSpacing(true);
        scoreBadge.setPadding(false);
        scoreBadge.setAlignItems(Alignment.CENTER);
        scoreBadge.addClassNames(
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.XSMALL);
        
        Icon scoreIcon = new Icon(VaadinIcon.CHART);
        scoreIcon.setColor("var(--lumo-primary-color)");
        scoreIcon.setSize("1em");
        
        Span scoreValue = new Span(scorePercent + "%");
        scoreValue.getStyle().set("font-weight", "bold");
        scoreValue.getStyle().set("font-size", "1.2em");
        scoreValue.getStyle().set("color", getScoreColor(scorePercent / 100.0));
        
        scoreBadge.add(scoreIcon, scoreValue);
        
        // Score label
        Span scoreLabel = new Span("Match Score");
        scoreLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.XSMALL, LumoUtility.Margin.Top.XSMALL);
        
        // Progress bar for score with enhanced styling
        ProgressBar scoreBar = new ProgressBar();
        scoreBar.setValue(scorePercent / 100.0);
        scoreBar.setWidth("120px");
        scoreBar.setHeight("10px");
        scoreBar.getStyle().set("--lumo-primary-color", getScoreColor(scorePercent / 100.0));
        scoreBar.getStyle().set("margin-top", "0.5em");
        
        scoreSection.add(scoreBadge, scoreLabel, scoreBar);
        
        header.add(contactInfo, scoreSection);
        
        // AI Analysis section with enhanced styling
        Div analysisSection = new Div();
        analysisSection.addClassNames(
                LumoUtility.Padding.Vertical.MEDIUM,
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Background.CONTRAST_5);
        
        // Analysis header with icon
        HorizontalLayout analysisHeader = new HorizontalLayout();
        analysisHeader.setSpacing(true);
        analysisHeader.setPadding(false);
        analysisHeader.setAlignItems(Alignment.CENTER);
        analysisHeader.setWidthFull();
        
        Icon aiIcon = new Icon(VaadinIcon.AUTOMATION);
        aiIcon.setColor("var(--lumo-primary-color)");
        aiIcon.setSize("1.2em");
        
        H4 analysisTitle = new H4("AI Analysis");
        analysisTitle.addClassNames(LumoUtility.Margin.NONE);
        
        // Add a button to toggle between summary and full analysis
        Button expandButton = new Button(new Icon(VaadinIcon.ANGLE_DOWN));
        expandButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        expandButton.getElement().setAttribute("aria-label", "Expand full analysis");
        expandButton.getStyle().set("margin-left", "auto");
        
        analysisHeader.add(aiIcon, analysisTitle, expandButton);
        
        // Create the analysis content container
        VerticalLayout analysisContent = new VerticalLayout();
        analysisContent.setPadding(false);
        analysisContent.setSpacing(false);
        analysisContent.setWidthFull();
        
        // Create the summary view (initially visible)
        VerticalLayout summaryView = createAnalysisSummaryView(match.getExplanation());
        
        // Create the full analysis view (initially hidden)
        VerticalLayout fullAnalysisView = createFullAnalysisView(match.getExplanation());
        fullAnalysisView.setVisible(false);
        
        // Add both views to the content container
        analysisContent.add(summaryView, fullAnalysisView);
        
        // Toggle between summary and full analysis when the expand button is clicked
        expandButton.addClickListener(e -> {
            boolean isExpanded = fullAnalysisView.isVisible();
            summaryView.setVisible(isExpanded);
            fullAnalysisView.setVisible(!isExpanded);
            
            // Update the button icon and aria-label
            if (isExpanded) {
                expandButton.setIcon(new Icon(VaadinIcon.ANGLE_DOWN));
                expandButton.getElement().setAttribute("aria-label", "Expand full analysis");
            } else {
                expandButton.setIcon(new Icon(VaadinIcon.ANGLE_UP));
                expandButton.getElement().setAttribute("aria-label", "Show summary");
            }
        });
        
        analysisSection.add(analysisHeader, analysisContent);
        
        // Actions section with enhanced styling
        HorizontalLayout actionsSection = new HorizontalLayout();
        actionsSection.setWidthFull();
        actionsSection.setJustifyContentMode(JustifyContentMode.END);
        actionsSection.addClassNames(
                LumoUtility.Padding.Top.MEDIUM,
                LumoUtility.Margin.Top.SMALL);
        
        Button viewButton = new Button("View Details", new Icon(VaadinIcon.EYE));
        viewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewButton.addClickListener(e -> viewResume(match.getResume().getId()));
        
        Button matchButton = new Button("Compare", new Icon(VaadinIcon.CHART));
        matchButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        matchButton.addClickListener(e -> {
            Notification.show("Detailed comparison view coming soon!", 
                    3000, Notification.Position.MIDDLE);
        });
        
        actionsSection.add(matchButton, viewButton);
        
        // Add all sections to the card
        card.add(header, analysisSection, actionsSection);
        
        return card;
    }
    
    /**
     * Creates a summary view of the analysis.
     */
    private VerticalLayout createAnalysisSummaryView(String explanation) {
        VerticalLayout summaryView = new VerticalLayout();
        summaryView.setPadding(false);
        summaryView.setSpacing(false);
        
        // Extract a summary from the explanation
        String analysisSummary = extractAnalysisSummary(explanation);
        
        // Split the analysis into key points
        String[] points = analysisSummary.split("\\. ");
        for (String point : points) {
            if (!point.trim().isEmpty()) {
                HorizontalLayout pointLayout = new HorizontalLayout();
                pointLayout.setSpacing(true);
                pointLayout.setPadding(false);
                pointLayout.setAlignItems(Alignment.START);
                
                Icon bulletIcon = new Icon(VaadinIcon.CHECK);
                bulletIcon.setColor("var(--lumo-success-color)");
                bulletIcon.setSize("0.8em");
                
                Paragraph pointText = new Paragraph(point.trim() + (point.endsWith(".") ? "" : "."));
                pointText.addClassNames(LumoUtility.TextColor.BODY, LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);
                pointText.getStyle().set("line-height", "1.4");
                
                pointLayout.add(bulletIcon, pointText);
                summaryView.add(pointLayout);
            }
        }
        
        return summaryView;
    }
    
    /**
     * Creates a full analysis view with proper formatting.
     */
    private VerticalLayout createFullAnalysisView(String explanation) {
        VerticalLayout fullAnalysisView = new VerticalLayout();
        fullAnalysisView.setPadding(false);
        fullAnalysisView.setSpacing(true);
        
        if (explanation == null || explanation.isEmpty()) {
            Paragraph noData = new Paragraph("No detailed analysis available.");
            noData.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            fullAnalysisView.add(noData);
            return fullAnalysisView;
        }
        
        try {
            // First, handle the main title if it exists
            Div mainTitleDiv = null;
            if (explanation.startsWith("# ")) {
                int endOfFirstLine = explanation.indexOf('\n');
                if (endOfFirstLine > 0) {
                    String mainTitle = explanation.substring(2, endOfFirstLine).trim();
                    mainTitleDiv = new Div();
                    mainTitleDiv.addClassNames(
                            LumoUtility.Padding.SMALL,
                            LumoUtility.Margin.Bottom.MEDIUM,
                            LumoUtility.BorderRadius.SMALL,
                            LumoUtility.Background.PRIMARY_10);
                    
                    H3 titleElement = new H3(mainTitle);
                    titleElement.addClassNames(
                            LumoUtility.Margin.NONE,
                            LumoUtility.TextAlignment.CENTER,
                            LumoUtility.FontSize.LARGE);
                    
                    mainTitleDiv.add(titleElement);
                    fullAnalysisView.add(mainTitleDiv);
                    
                    // Remove the main title from the explanation for further processing
                    explanation = explanation.substring(endOfFirstLine + 1).trim();
                }
            }
            
            // Process the markdown-formatted explanation by sections
            // Use a regex to split by ## headers while keeping the headers
            String[] parts = explanation.split("(?=## )");
            
            for (String part : parts) {
                if (part.trim().isEmpty()) continue;
                
                // Create a section container
                Div sectionDiv = new Div();
                sectionDiv.addClassNames(
                        LumoUtility.Padding.SMALL,
                        LumoUtility.Margin.Bottom.SMALL,
                        LumoUtility.BorderRadius.SMALL);
                
                // Extract section title and content
                String sectionTitle;
                String sectionContent;
                
                if (part.startsWith("## ")) {
                    int endOfTitleLine = part.indexOf('\n');
                    if (endOfTitleLine > 0) {
                        sectionTitle = part.substring(3, endOfTitleLine).trim();
                        sectionContent = part.substring(endOfTitleLine + 1).trim();
                    } else {
                        sectionTitle = part.substring(3).trim();
                        sectionContent = "";
                    }
                } else {
                    // If there's no ## header, use a default title
                    sectionTitle = "Analysis";
                    sectionContent = part.trim();
                }
                
                // Add section title
                H4 titleElement = new H4(sectionTitle);
                titleElement.addClassNames(
                        LumoUtility.Margin.NONE,
                        LumoUtility.Margin.Bottom.XSMALL,
                        LumoUtility.FontSize.MEDIUM);
                
                // Set section color based on title
                String sectionColor = getSectionColor(sectionTitle);
                sectionDiv.getStyle().set("border-left", "4px solid " + sectionColor);
                sectionDiv.getStyle().set("background-color", sectionColor + "10"); // 10% opacity
                
                sectionDiv.add(titleElement);
                
                // Process section content
                if (!sectionContent.isEmpty()) {
                    // Handle bullet points (both • and * are supported)
                    if (sectionContent.contains("• ") || sectionContent.contains("* ")) {
                        // Split by lines first
                        String[] lines = sectionContent.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            
                            if (line.startsWith("• ") || line.startsWith("* ")) {
                                // This is a bullet point
                                String bulletText = line.startsWith("• ") ? 
                                        line.substring(2).trim() : line.substring(2).trim();
                                
                                HorizontalLayout bulletLayout = new HorizontalLayout();
                                bulletLayout.setSpacing(true);
                                bulletLayout.setPadding(false);
                                bulletLayout.setAlignItems(Alignment.START);
                                
                                Icon bulletIcon = new Icon(VaadinIcon.CIRCLE);
                                bulletIcon.setColor(sectionColor);
                                bulletIcon.setSize("0.5em");
                                
                                Paragraph bulletContent = new Paragraph(bulletText);
                                bulletContent.addClassNames(
                                        LumoUtility.TextColor.BODY,
                                        LumoUtility.Margin.NONE,
                                        LumoUtility.FontSize.SMALL);
                                bulletContent.getStyle().set("line-height", "1.4");
                                
                                bulletLayout.add(bulletIcon, bulletContent);
                                sectionDiv.add(bulletLayout);
                            } else {
                                // Regular text line
                                Paragraph paragraphElement = new Paragraph(line);
                                paragraphElement.addClassNames(
                                        LumoUtility.TextColor.BODY,
                                        LumoUtility.Margin.NONE,
                                        LumoUtility.Margin.Bottom.XSMALL,
                                        LumoUtility.FontSize.SMALL);
                                paragraphElement.getStyle().set("line-height", "1.4");
                                sectionDiv.add(paragraphElement);
                            }
                        }
                    } else {
                        // Regular paragraph text
                        String[] paragraphs = sectionContent.split("\n\n");
                        for (String paragraph : paragraphs) {
                            if (!paragraph.trim().isEmpty()) {
                                Paragraph paragraphElement = new Paragraph(paragraph.trim());
                                paragraphElement.addClassNames(
                                        LumoUtility.TextColor.BODY,
                                        LumoUtility.Margin.NONE,
                                        LumoUtility.Margin.Bottom.XSMALL,
                                        LumoUtility.FontSize.SMALL);
                                paragraphElement.getStyle().set("line-height", "1.4");
                                sectionDiv.add(paragraphElement);
                            }
                        }
                    }
                }
                
                fullAnalysisView.add(sectionDiv);
            }
        } catch (Exception e) {
            // If any error occurs during parsing, display the raw text
            Paragraph errorMessage = new Paragraph("Error parsing analysis. Displaying raw content:");
            errorMessage.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
            
            Paragraph rawContent = new Paragraph(explanation);
            rawContent.addClassNames(LumoUtility.TextColor.BODY, LumoUtility.FontSize.SMALL);
            rawContent.getStyle().set("white-space", "pre-wrap");
            
            fullAnalysisView.add(errorMessage, rawContent);
        }
        
        return fullAnalysisView;
    }
    
    /**
     * Gets an appropriate color for a section based on its title.
     */
    private String getSectionColor(String sectionTitle) {
        sectionTitle = sectionTitle.toUpperCase();
        
        if (sectionTitle.contains("EXECUTIVE") || sectionTitle.contains("SUMMARY")) {
            return "var(--lumo-primary-color)";
        } else if (sectionTitle.contains("MATCH SCORE")) {
            return "var(--lumo-success-color)";
        } else if (sectionTitle.contains("STRENGTH") || sectionTitle.contains("KEY")) {
            return "var(--lumo-success-color)";
        } else if (sectionTitle.contains("IMPROVEMENT") || sectionTitle.contains("WEAKNESS")) {
            return "var(--lumo-error-color)";
        } else if (sectionTitle.contains("DETAIL") || sectionTitle.contains("CATEGORY")) {
            return "var(--lumo-contrast-70pct)";
        } else if (sectionTitle.contains("RECOMMENDATION") || sectionTitle.contains("HIRING")) {
            return "var(--lumo-primary-color)";
        } else {
            return "var(--lumo-contrast-50pct)";
        }
    }
    
    /**
     * Extracts a summary from the AI explanation.
     */
    private String extractAnalysisSummary(String explanation) {
        if (explanation == null || explanation.isEmpty()) {
            return "No analysis available.";
        }
        
        // Try to find a summary section
        String[] lines = explanation.split("\n");
        StringBuilder summary = new StringBuilder();
        boolean inSummarySection = false;
        
        for (String line : lines) {
            if (line.contains("SUMMARY") || line.contains("Summary") || line.contains("summary")) {
                inSummarySection = true;
                continue;
            }
            
            if (inSummarySection && !line.trim().isEmpty()) {
                if (line.contains("MATCH SCORE") || line.contains("Match Score") || line.contains("SKILLS MATCH")) {
                    break;
                }
                summary.append(line.trim()).append(" ");
            }
        }
        
        // If we couldn't find a summary section, just take the first few sentences
        if (summary.length() == 0) {
            String text = explanation.replaceAll("\n", " ").trim();
            String[] sentences = text.split("[.!?]");
            
            for (int i = 0; i < Math.min(3, sentences.length); i++) {
                if (!sentences[i].trim().isEmpty()) {
                    summary.append(sentences[i].trim()).append(". ");
                }
            }
        }
        
        return summary.length() > 0 ? summary.toString() : "This candidate has relevant experience for the position.";
    }
    
    /**
     * Sorts the results by score.
     */
    private void sortResults() {
        VerticalLayout cardsContainer = (VerticalLayout) resultsLayout.getComponentAt(2);
        cardsContainer.removeAll();
        
        // Get the current matches and sort them
        List<ResumeMatch> matches = new ArrayList<>(currentMatches);
        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // Add cards for each match
        for (ResumeMatch match : matches) {
            cardsContainer.add(createResumeMatchCard(match));
        }
    }

    private void findMatches() {
        String jobDescription = jobDescriptionArea.getValue();
        
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            Notification notification = new Notification(
                    "Please enter a job description", 
                    3000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
            return;
        }
        
        try {
            // Show loading indicator
            progressBar.setVisible(true);
            matchButton.setEnabled(false);
            matchButton.setText("Finding matches...");
            
            // Find matches
            List<ResumeMatch> matches = resumeMatchingService.findMatchingResumes(jobDescription, 10);
            
            // Display results
            showResults(matches);
            
            // Reset UI
            progressBar.setVisible(false);
            matchButton.setEnabled(true);
            matchButton.setText("Find Matching Resumes");
            
        } catch (Exception e) {
            // Reset UI
            progressBar.setVisible(false);
            matchButton.setEnabled(true);
            matchButton.setText("Find Matching Resumes");
            
            // Show error notification
            Notification notification = new Notification(
                    "Error finding matches: " + e.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        }
    }

    // Store current matches for sorting
    private List<ResumeMatch> currentMatches = new ArrayList<>();
    
    private void showResults(List<ResumeMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            Notification notification = new Notification(
                    "No matching resumes found", 
                    3000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
            notification.open();
            resultsLayout.setVisible(false);
            return;
        }
        
        // Store current matches
        currentMatches = new ArrayList<>(matches);
        
        // Get the cards container
        VerticalLayout cardsContainer = (VerticalLayout) resultsLayout.getComponentAt(2);
        cardsContainer.removeAll();
        
        // Add cards for each match
        for (ResumeMatch match : matches) {
            cardsContainer.add(createResumeMatchCard(match));
        }
        
        // Show results
        resultsLayout.setVisible(true);
        
        // Show success notification
        Notification notification = new Notification(
                "Found " + matches.size() + " matching resumes", 
                3000, 
                Notification.Position.MIDDLE
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    private void viewResume(UUID id) {
        getUI().ifPresent(ui -> ui.navigate("resume/" + id));
    }

    private String getScoreColor(double score) {
        if (score >= 0.8) {
            return "var(--lumo-success-color)";
        } else if (score >= 0.6) {
            return "var(--lumo-primary-color)";
        } else if (score >= 0.4) {
            return "var(--lumo-contrast-60pct)";
        } else {
            return "var(--lumo-error-color)";
        }
    }
    
    /**
     * Creates a breadcrumb navigation component.
     * Note: This is a placeholder for future implementation.
     */
}
