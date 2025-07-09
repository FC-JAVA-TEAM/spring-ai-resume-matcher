package com.telus.spring.ai.resume.service.ui;

import com.telus.spring.ai.resume.ui.MainLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.telus.spring.ai.resume.model.CandidateEvaluationModel;
import com.telus.spring.ai.resume.model.ResumeAnalysis;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.service.ResumeMatchingService;
import com.telus.spring.ai.resume.service.ResumeStorageService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
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
    
    // Store current matches for sorting
    private List<ResumeMatch> currentMatches = new ArrayList<>();

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
        
        // Add a checkbox for locking/unlocking the resume
        Checkbox lockCheckbox = new Checkbox("Lock");
        lockCheckbox.addValueChangeListener(event -> {
            boolean isChecked = event.getValue();
            handleLockStatusChange(match, isChecked);
        });
        
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
        VerticalLayout summaryView = createAnalysisSummaryView(match.getExplanation(), match.getAnalysis());
        
        // Create the full analysis view (initially hidden)
        VerticalLayout fullAnalysisView = createFullAnalysisView(match.getExplanation(), match.getAnalysis());
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
        card.add(lockCheckbox, header, analysisSection, actionsSection);
        
        return card;
    }
    
    /**
     * Creates a summary view of the analysis.
     */
    private VerticalLayout createAnalysisSummaryView(String explanation, ResumeAnalysis analysis) {
        VerticalLayout summaryView = new VerticalLayout();
        summaryView.setPadding(false);
        summaryView.setSpacing(false);
        
        // If we have structured analysis data, use the executive summary
        if (analysis != null && analysis.getExecutiveSummary() != null) {
            Paragraph summary = new Paragraph(analysis.getExecutiveSummary());
            summary.addClassNames(LumoUtility.TextColor.BODY, LumoUtility.FontSize.SMALL);
            summary.getStyle().set("line-height", "1.4");
            summaryView.add(summary);
            
            // Add overall score
            if (analysis.getOverallScore() != null) {
                HorizontalLayout scoreLayout = new HorizontalLayout();
                scoreLayout.setSpacing(true);
                scoreLayout.setPadding(false);
                scoreLayout.setAlignItems(Alignment.CENTER);
                scoreLayout.addClassNames(LumoUtility.Margin.Top.MEDIUM);
                
                Icon scoreIcon = new Icon(VaadinIcon.CHART);
                scoreIcon.setColor("var(--lumo-primary-color)");
                scoreIcon.setSize("1em");
                
                Span scoreLabel = new Span("Overall Match Score: ");
                scoreLabel.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
                
                Span scoreValue = new Span(analysis.getOverallScore() + "%");
                scoreValue.getStyle().set("color", getScoreColor(analysis.getOverallScore() / 100.0));
                scoreValue.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
                
                scoreLayout.add(scoreIcon, scoreLabel, scoreValue);
                summaryView.add(scoreLayout);
            }
            
            return summaryView;
        }
        
        // Fallback to text parsing if no structured data is available and explanation is available
        if (explanation == null || explanation.isEmpty()) {
            Paragraph noData = new Paragraph("No analysis data available.");
            noData.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            summaryView.add(noData);
            return summaryView;
        }
        
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
    private VerticalLayout createFullAnalysisView(String explanation, ResumeAnalysis analysis) {
        VerticalLayout fullAnalysisView = new VerticalLayout();
        fullAnalysisView.setPadding(false);
        fullAnalysisView.setSpacing(true);
        
        // If we have structured analysis data, use that instead of parsing the raw text
        if (analysis != null) {
            return createStructuredAnalysisView(analysis);
        }
        
        // Fallback to text parsing if no structured data is available
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
     * Creates a structured analysis view based on the ResumeAnalysis object.
     */
    private VerticalLayout createStructuredAnalysisView(ResumeAnalysis analysis) {
        VerticalLayout structuredView = new VerticalLayout();
        structuredView.setPadding(false);
        structuredView.setSpacing(true);
        
        // Executive Summary Section
        if (analysis.getExecutiveSummary() != null) {
            Div summarySection = new Div();
            summarySection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            summarySection.getStyle().set("border-left", "4px solid var(--lumo-primary-color)");
            summarySection.getStyle().set("background-color", "var(--lumo-primary-color-10%)");
            
            H4 summaryTitle = new H4("Executive Summary");
            summaryTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.XSMALL,
                    LumoUtility.FontSize.MEDIUM);
            
            Paragraph summaryText = new Paragraph(analysis.getExecutiveSummary());
            summaryText.addClassNames(
                    LumoUtility.TextColor.BODY,
                    LumoUtility.Margin.NONE,
                    LumoUtility.FontSize.SMALL);
            summaryText.getStyle().set("line-height", "1.4");
            
            summarySection.add(summaryTitle, summaryText);
            structuredView.add(summarySection);
        }
        
        // Overall Score Section
        if (analysis.getOverallScore() != null) {
            Div scoreSection = new Div();
            scoreSection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            scoreSection.getStyle().set("border-left", "4px solid var(--lumo-primary-color)");
            scoreSection.getStyle().set("background-color", "var(--lumo-primary-color-10%)");
            
            H4 scoreTitle = new H4("Overall Match Score");
            scoreTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.MEDIUM,
                    LumoUtility.FontSize.MEDIUM);
            
            // Create a circular progress indicator
            Div scoreIndicator = new Div();
            scoreIndicator.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.JustifyContent.CENTER,
                    LumoUtility.Margin.Horizontal.AUTO);
            
            int score = analysis.getOverallScore();
            String scoreColor = getScoreColor(score / 100.0);
            
            Div circleWrapper = new Div();
            circleWrapper.getStyle().set("position", "relative");
            circleWrapper.getStyle().set("width", "120px");
            circleWrapper.getStyle().set("height", "120px");
            
            Div circle = new Div();
            circle.getStyle().set("border-radius", "50%");
            circle.getStyle().set("width", "100%");
            circle.getStyle().set("height", "100%");
            circle.getStyle().set("border", "10px solid var(--lumo-contrast-10%)");
            circle.getStyle().set("border-top", "10px solid " + scoreColor);
            circle.getStyle().set("transform", "rotate(" + (score * 3.6) + "deg)");
            circle.getStyle().set("box-sizing", "border-box");
            
            Div scoreValue = new Div();
            scoreValue.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.JustifyContent.CENTER);
            scoreValue.getStyle().set("position", "absolute");
            scoreValue.getStyle().set("top", "50%");
            scoreValue.getStyle().set("left", "50%");
            scoreValue.getStyle().set("transform", "translate(-50%, -50%)");
            
            Span scoreText = new Span(score + "%");
            scoreText.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.XLARGE);
            scoreText.getStyle().set("color", scoreColor);
            
            scoreValue.add(scoreText);
            circleWrapper.add(circle, scoreValue);
            scoreIndicator.add(circleWrapper);
            
            // Add score description
            Paragraph scoreDescription = new Paragraph(getScoreDescription(score));
            scoreDescription.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.TextAlignment.CENTER,
                    LumoUtility.Margin.Top.MEDIUM);
            
            scoreSection.add(scoreTitle, scoreIndicator, scoreDescription);
            structuredView.add(scoreSection);
        }
        
        // Category Scores Section
        if (analysis.getCategoryScores() != null) {
            Div scoresSection = new Div();
            scoresSection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            scoresSection.getStyle().set("border-left", "4px solid var(--lumo-contrast-70pct)");
            scoresSection.getStyle().set("background-color", "var(--lumo-contrast-5%)");
            
            H4 scoresTitle = new H4("Category Scores");
            scoresTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.MEDIUM,
                    LumoUtility.FontSize.MEDIUM);
            
            scoresSection.add(scoresTitle);
            
            // Add category scores with correct maximum values based on the prompt weights
            addCategoryScore(scoresSection, "Technical Skills", analysis.getCategoryScores().getTechnicalSkills(), 40);
            addCategoryScore(scoresSection, "Experience", analysis.getCategoryScores().getExperience(), 25);
            addCategoryScore(scoresSection, "Education", analysis.getCategoryScores().getEducation(), 10);
            addCategoryScore(scoresSection, "Soft Skills", analysis.getCategoryScores().getSoftSkills(), 15);
            addCategoryScore(scoresSection, "Achievements", analysis.getCategoryScores().getAchievements(), 10);
            
            structuredView.add(scoresSection);
        }
        
        // Skill Explanations Section
        if (analysis.getSkillExplanations() != null && !analysis.getSkillExplanations().isEmpty()) {
            Div skillsSection = new Div();
            skillsSection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            skillsSection.getStyle().set("border-left", "4px solid var(--lumo-primary-color)");
            skillsSection.getStyle().set("background-color", "var(--lumo-primary-color-10%)");
            
            H4 skillsTitle = new H4("Technical Skills Explained");
            skillsTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.XSMALL,
                    LumoUtility.FontSize.MEDIUM);
            
            skillsSection.add(skillsTitle);
            
            // Add each skill explanation
            for (Map.Entry<String, String> entry : analysis.getSkillExplanations().entrySet()) {
                HorizontalLayout skillLayout = new HorizontalLayout();
                skillLayout.setSpacing(true);
                skillLayout.setPadding(false);
                skillLayout.setAlignItems(Alignment.START);
                
                Icon skillIcon = new Icon(VaadinIcon.CODE);
                skillIcon.setColor("var(--lumo-primary-color)");
                skillIcon.setSize("1em");
                
                VerticalLayout skillContent = new VerticalLayout();
                skillContent.setPadding(false);
                skillContent.setSpacing(false);
                
                Span skillName = new Span(entry.getKey());
                skillName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
                
                Paragraph explanationText = new Paragraph(entry.getValue());
                explanationText.addClassNames(
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.Margin.NONE,
                        LumoUtility.FontSize.XSMALL);
                explanationText.getStyle().set("line-height", "1.4");
                
                skillContent.add(skillName, explanationText);
                skillLayout.add(skillIcon, skillContent);
                skillsSection.add(skillLayout);
            }
            
            structuredView.add(skillsSection);
        }
        
        // Key Strengths Section
        if (analysis.getKeyStrengths() != null && !analysis.getKeyStrengths().isEmpty()) {
            Div strengthsSection = new Div();
            strengthsSection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            strengthsSection.getStyle().set("border-left", "4px solid var(--lumo-success-color)");
            strengthsSection.getStyle().set("background-color", "var(--lumo-success-color-10%)");
            
            H4 strengthsTitle = new H4("Key Strengths");
            strengthsTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.XSMALL,
                    LumoUtility.FontSize.MEDIUM);
            
            strengthsSection.add(strengthsTitle);
            
            // Add each strength
            for (ResumeAnalysis.KeyStrength strength : analysis.getKeyStrengths()) {
                HorizontalLayout strengthLayout = new HorizontalLayout();
                strengthLayout.setSpacing(true);
                strengthLayout.setPadding(false);
                strengthLayout.setAlignItems(Alignment.START);
                
                Icon bulletIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
                bulletIcon.setColor("var(--lumo-success-color)");
                bulletIcon.setSize("1em");
                
                VerticalLayout strengthContent = new VerticalLayout();
                strengthContent.setPadding(false);
                strengthContent.setSpacing(false);
                
                Span strengthText = new Span(strength.getStrength());
                strengthText.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
                
                Paragraph evidenceText = new Paragraph(strength.getEvidence());
                evidenceText.addClassNames(
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.Margin.NONE,
                        LumoUtility.FontSize.XSMALL);
                evidenceText.getStyle().set("line-height", "1.4");
                
                strengthContent.add(strengthText, evidenceText);
                strengthLayout.add(bulletIcon, strengthContent);
                strengthsSection.add(strengthLayout);
            }
            
            structuredView.add(strengthsSection);
        }
        
        // Improvement Areas Section
        if (analysis.getImprovementAreas() != null && !analysis.getImprovementAreas().isEmpty()) {
            Div improvementSection = new Div();
            improvementSection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            improvementSection.getStyle().set("border-left", "4px solid var(--lumo-error-color)");
            improvementSection.getStyle().set("background-color", "var(--lumo-error-color-10%)");
            
            H4 improvementTitle = new H4("Areas for Improvement");
            improvementTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.XSMALL,
                    LumoUtility.FontSize.MEDIUM);
            
            improvementSection.add(improvementTitle);
            
            // Add each improvement area
            for (ResumeAnalysis.ImprovementArea area : analysis.getImprovementAreas()) {
                HorizontalLayout areaLayout = new HorizontalLayout();
                areaLayout.setSpacing(true);
                areaLayout.setPadding(false);
                areaLayout.setAlignItems(Alignment.START);
                
                Icon bulletIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE);
                bulletIcon.setColor("var(--lumo-error-color)");
                bulletIcon.setSize("1em");
                
                VerticalLayout areaContent = new VerticalLayout();
                areaContent.setPadding(false);
                areaContent.setSpacing(false);
                
                Span gapText = new Span(area.getGap());
                gapText.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
                
                Paragraph suggestionText = new Paragraph(area.getSuggestion());
                suggestionText.addClassNames(
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.Margin.NONE,
                        LumoUtility.FontSize.XSMALL);
                suggestionText.getStyle().set("line-height", "1.4");
                
                areaContent.add(gapText, suggestionText);
                areaLayout.add(bulletIcon, areaContent);
                improvementSection.add(areaLayout);
            }
            
            structuredView.add(improvementSection);
        }
        
        // Recommendation Section
        if (analysis.getRecommendation() != null) {
            Div recommendationSection = new Div();
            recommendationSection.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.BorderRadius.SMALL);
            recommendationSection.getStyle().set("border-left", "4px solid var(--lumo-primary-color)");
            recommendationSection.getStyle().set("background-color", "var(--lumo-primary-color-10%)");
            
            H4 recommendationTitle = new H4("Recommendation");
            recommendationTitle.addClassNames(
                    LumoUtility.Margin.NONE,
                    LumoUtility.Margin.Bottom.XSMALL,
                    LumoUtility.FontSize.MEDIUM);
            
            HorizontalLayout recommendationLayout = new HorizontalLayout();
            recommendationLayout.setSpacing(true);
            recommendationLayout.setPadding(false);
            recommendationLayout.setAlignItems(Alignment.START);
            
            // Choose icon based on recommendation type
            Icon recommendationIcon;
            String iconColor;
            if ("Match".equalsIgnoreCase(analysis.getRecommendation().getType()) || 
                "Strong Match".equalsIgnoreCase(analysis.getRecommendation().getType())) {
                recommendationIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
                iconColor = "var(--lumo-success-color)";
            } else if ("Potential Match".equalsIgnoreCase(analysis.getRecommendation().getType())) {
                recommendationIcon = new Icon(VaadinIcon.QUESTION_CIRCLE);
                iconColor = "var(--lumo-primary-color)";
            } else {
                recommendationIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
                iconColor = "var(--lumo-error-color)";
            }
            recommendationIcon.setColor(iconColor);
            recommendationIcon.setSize("1em");
            
            VerticalLayout recommendationContent = new VerticalLayout();
            recommendationContent.setPadding(false);
            recommendationContent.setSpacing(false);
            
            Span typeText = new Span(analysis.getRecommendation().getType());
            typeText.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
            
            Paragraph reasonText = new Paragraph(analysis.getRecommendation().getReason());
            reasonText.addClassNames(
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.Margin.NONE,
                    LumoUtility.FontSize.XSMALL);
            reasonText.getStyle().set("line-height", "1.4");
            
            recommendationContent.add(typeText, reasonText);
            recommendationLayout.add(recommendationIcon, recommendationContent);
            
            recommendationSection.add(recommendationTitle, recommendationLayout);
            structuredView.add(recommendationSection);
        }
        
        return structuredView;
    }
    
    /**
     * Adds a category score bar to the given container.
     */
    private void addCategoryScore(Div container, String categoryName, Integer score, int maxScore) {
        if (score == null) return;
        
        HorizontalLayout categoryLayout = new HorizontalLayout();
        categoryLayout.setWidthFull();
        categoryLayout.setAlignItems(Alignment.CENTER);
        categoryLayout.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        
        // Category name
        Span nameLabel = new Span(categoryName);
        nameLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        nameLabel.setWidth("120px");
        
        // Score bar
        ProgressBar scoreBar = new ProgressBar();
        scoreBar.setValue((double) score / maxScore);
        scoreBar.setWidthFull();
        scoreBar.setHeight("8px");
        scoreBar.getStyle().set("--lumo-primary-color", getCategoryColor(categoryName));
        
        // Score value
        Span scoreValue = new Span(score + "/" + maxScore);
        scoreValue.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
        scoreValue.setWidth("50px");
        scoreValue.getStyle().set("text-align", "right");
        
        categoryLayout.add(nameLabel, scoreBar, scoreValue);
        container.add(categoryLayout);
    }
    
    /**
     * Gets a color for a category based on its name.
     */
    private String getCategoryColor(String categoryName) {
        categoryName = categoryName.toUpperCase();
        
        if (categoryName.contains("TECHNICAL") || categoryName.contains("SKILL")) {
            return "var(--lumo-primary-color)";
        } else if (categoryName.contains("EXPERIENCE")) {
            return "var(--lumo-success-color)";
        } else if (categoryName.contains("EDUCATION")) {
            return "var(--lumo-contrast-70pct)";
        } else if (categoryName.contains("SOFT") || categoryName.contains("COMMUNICATION")) {
            return "var(--lumo-tertiary-text-color)";
        } else if (categoryName.contains("ACHIEVEMENT")) {
            return "var(--lumo-error-color)";
        } else {
            return "var(--lumo-contrast-50pct)";
        }
    }
    
    /**
     * Gets a color based on score percentage.
     */
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
     * Gets a description based on score.
     */
    private String getScoreDescription(int score) {
        if (score >= 80) {
            return "Excellent match for the position";
        } else if (score >= 60) {
            return "Good match with relevant qualifications";
        } else if (score >= 40) {
            return "Potential match with some relevant experience";
        } else {
            return "Limited match for this position";
        }
    }
    
    /**
     * Extracts a summary from the analysis text.
     */
    private String extractAnalysisSummary(String explanation) {
        if (explanation == null || explanation.isEmpty()) {
            return "No analysis available.";
        }
        
        // Try to extract the executive summary section
        int summaryStart = explanation.indexOf("## Executive Summary");
        if (summaryStart >= 0) {
            int nextSectionStart = explanation.indexOf("##", summaryStart + 5);
            if (nextSectionStart > summaryStart) {
                return explanation.substring(summaryStart + 20, nextSectionStart).trim();
            } else {
                return explanation.substring(summaryStart + 20).trim();
            }
        }
        
        // If no executive summary, try to extract the first paragraph
        int firstParagraphEnd = explanation.indexOf("\n\n");
        if (firstParagraphEnd > 0) {
            return explanation.substring(0, firstParagraphEnd).trim();
        }
        
        // If all else fails, return the first 200 characters
        return explanation.length() > 200 ? 
                explanation.substring(0, 200) + "..." : explanation;
    }
    
    /**
     * Navigates to the resume detail view.
     */
    private void viewResume(UUID resumeId) {
        getUI().ifPresent(ui -> ui.navigate("resume/" + resumeId));
    }
    
    /**
     * Finds matching resumes for the job description.
     */
    private void findMatches() {
        String jobDescription = jobDescriptionArea.getValue();
        
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            Notification notification = new Notification(
                    "Please enter a job description", 
                    3000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
            return;
        }
        
        // Show progress bar and disable button
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        matchButton.setEnabled(false);
        
        try {
            // Call the service to find matches
            List<ResumeMatch> matches = resumeMatchingService.findMatchingResumes(jobDescription, 10);
            
            // Store the matches for sorting
            currentMatches = new ArrayList<>(matches);
            
            // Show the results
            showResults(matches);
            
        } catch (Exception e) {
            // Show error notification
            Notification notification = new Notification(
                    "Error finding matches: " + e.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        } finally {
            // Hide progress bar and enable button
            progressBar.setVisible(false);
            matchButton.setEnabled(true);
        }
    }
    
    /**
     * Shows the matching results.
     */
    private void showResults(List<ResumeMatch> matches) {
        // Clear previous results
        VerticalLayout cardsContainer = (VerticalLayout) resultsLayout.getComponentAt(2);
        cardsContainer.removeAll();
        
        // Show the results layout
        resultsLayout.setVisible(true);
        
        // If no matches found, show a message
        if (matches == null || matches.isEmpty()) {
            Div noResultsDiv = new Div();
            noResultsDiv.addClassNames(
                    LumoUtility.Padding.LARGE,
                    LumoUtility.TextAlignment.CENTER,
                    LumoUtility.Background.CONTRAST_5,
                    LumoUtility.BorderRadius.MEDIUM);
            
            Icon icon = new Icon(VaadinIcon.SEARCH);
            icon.setSize("3em");
            icon.setColor("var(--lumo-contrast-30pct)");
            
            H3 title = new H3("No Matching Resumes Found");
            title.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.XSMALL);
            
            Paragraph message = new Paragraph("Try adjusting your job description or adding more resumes to the system.");
            message.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.NONE);
            
            noResultsDiv.add(icon, title, message);
            cardsContainer.add(noResultsDiv);
            return;
        }
        
        // Add a card for each match
        for (ResumeMatch match : matches) {
            VerticalLayout card = createResumeMatchCard(match);
            cardsContainer.add(card);
        }
    }
    
    /**
     * Sorts the results by score.
     */
    private void sortResults() {
        if (currentMatches == null || currentMatches.isEmpty()) {
            return;
        }
        
        // Sort the matches by score in descending order
        currentMatches.sort((a, b) -> {
            Integer scoreA = a.getScore();
            Integer scoreB = b.getScore();
            
            if (scoreA == null) scoreA = 0;
            if (scoreB == null) scoreB = 0;
            
            return scoreB.compareTo(scoreA);
        });
        
        // Show the sorted results
        showResults(currentMatches);
    }
    
    /**
     * Handles the lock status change for a resume match.
     */
    private void handleLockStatusChange(ResumeMatch match, boolean locked) {
        // Convert ResumeMatch to CandidateEvaluationDTO
        CandidateEvaluationModel dto = convertToCandidateEvaluationDTO(match);
        dto.setLocked(locked);
        
        // For now, just display the DTO
        displayCandidateEvaluation(dto);
        
        // In the future, this would call a service to persist the lock status
    }
    
    /**
     * Converts a ResumeMatch to a CandidateEvaluationDTO.
     */
    private CandidateEvaluationModel convertToCandidateEvaluationDTO(ResumeMatch match) {
        CandidateEvaluationModel dto = new CandidateEvaluationModel();
        
        // Set basic resume information
        dto.setResumeId(match.getResume().getId());
        dto.setName(match.getResume().getName());
        dto.setEmail(match.getResume().getEmail());
        dto.setPhoneNumber(match.getResume().getPhoneNumber());
        
        // Set score
        dto.setScore(match.getScore() != null ? match.getScore() : 0);
        
        // Extract data from ResumeAnalysis if available
        ResumeAnalysis analysis = match.getAnalysis();
        if (analysis != null) {
            // Executive summary
            dto.setExecutiveSummary(analysis.getExecutiveSummary());
            
            // Key strengths
            List<String> keyStrengths = new ArrayList<>();
            if (analysis.getKeyStrengths() != null) {
                for (ResumeAnalysis.KeyStrength strength : analysis.getKeyStrengths()) {
                    keyStrengths.add(strength.getStrength());
                }
            }
            dto.setKeyStrengths(keyStrengths);
            
            // Improvement areas
            List<String> improvementAreas = new ArrayList<>();
            if (analysis.getImprovementAreas() != null) {
                for (ResumeAnalysis.ImprovementArea area : analysis.getImprovementAreas()) {
                    improvementAreas.add(area.getGap());
                }
            }
            dto.setImprovementAreas(improvementAreas);
            
            // Category scores
            if (analysis.getCategoryScores() != null) {
            //    dto.setTechnicalSkills(analysis.ge != null ? 
               //     analysis.getCategoryScores().getTechnicalSkills() : 0);
                dto.setExperience(analysis.getCategoryScores().getExperience() != null ? 
                    analysis.getCategoryScores().getExperience() : 0);
                dto.setEducation(analysis.getCategoryScores().getEducation() != null ? 
                    analysis.getCategoryScores().getEducation() : 0);
                dto.setSoftSkills(analysis.getCategoryScores().getSoftSkills() != null ? 
                    analysis.getCategoryScores().getSoftSkills() : 0);
                dto.setAchievements(analysis.getCategoryScores().getAchievements() != null ? 
                    analysis.getCategoryScores().getAchievements() : 0);
            }
            
            // Recommendation
            if (analysis.getRecommendation() != null) {
                dto.setRecommendationType(analysis.getRecommendation().getType());
                dto.setRecommendationReason(analysis.getRecommendation().getReason());
            }
        } else if (match.getExplanation() != null) {
            // If no structured analysis is available, use the explanation text
            dto.setExecutiveSummary(extractAnalysisSummary(match.getExplanation()));
        }
        
        // Default locked status
        dto.setLocked(false);
        
        return dto;
    }
    
    /**
     * Displays the CandidateEvaluationDTO in a dialog.
     */
    private void displayCandidateEvaluation(CandidateEvaluationModel dto) {
        // Create a dialog to display the DTO
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        
        H3 title = new H3("Candidate Evaluation");
        
        // Create a formatted representation of the DTO
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='white-space: pre-wrap;'>");
        sb.append("<strong>Name:</strong> ").append(dto.getName()).append("<br>");
        sb.append("<strong>Email:</strong> ").append(dto.getEmail()).append("<br>");
        sb.append("<strong>Phone:</strong> ").append(dto.getPhoneNumber()).append("<br>");
        sb.append("<strong>Resume ID:</strong> ").append(dto.getResumeId()).append("<br>");
        sb.append("<strong>Score:</strong> ").append(dto.getScore()).append("<br>");
        sb.append("<strong>Executive Summary:</strong> ").append(dto.getExecutiveSummary()).append("<br>");
        
        sb.append("<strong>Key Strengths:</strong><br>");
        if (dto.getKeyStrengths() == null || dto.getKeyStrengths().isEmpty()) {
            sb.append("- None<br>");
        } else {
            for (String strength : dto.getKeyStrengths()) {
                sb.append("- ").append(strength).append("<br>");
            }
        }
        
        sb.append("<strong>Improvement Areas:</strong><br>");
        if (dto.getImprovementAreas() == null || dto.getImprovementAreas().isEmpty()) {
            sb.append("- None<br>");
        } else {
            for (String area : dto.getImprovementAreas()) {
                sb.append("- ").append(area).append("<br>");
            }
        }
        
        sb.append("<strong>Category Scores:</strong><br>");
        sb.append("- Technical Skills: ").append(dto.getTechnicalSkills()).append("<br>");
        sb.append("- Experience: ").append(dto.getExperience()).append("<br>");
        sb.append("- Education: ").append(dto.getEducation()).append("<br>");
        sb.append("- Soft Skills: ").append(dto.getSoftSkills()).append("<br>");
        sb.append("- Achievements: ").append(dto.getAchievements()).append("<br>");
        
        sb.append("<strong>Recommendation:</strong> ").append(dto.getRecommendationType()).append("<br>");
        sb.append("<strong>Reason:</strong> ").append(dto.getRecommendationReason()).append("<br>");
        sb.append("<strong>Locked:</strong> ").append(dto.isLocked() ? "Yes" : "No").append("<br>");
        sb.append("</div>");
        
        Div formattedContent = new Div();
        formattedContent.getElement().setProperty("innerHTML", sb.toString());
        
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        content.add(title, formattedContent, closeButton);
        dialog.add(content);
        
        dialog.open();
    }
}
