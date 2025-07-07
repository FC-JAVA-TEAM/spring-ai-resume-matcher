package com.telus.spring.ai.resume.service.ui;

import com.telus.spring.ai.resume.ui.MainLayout;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeAnalysis;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.service.ResumeMatchingService;
import com.telus.spring.ai.resume.service.ResumeStorageService;

import java.util.List;
import java.util.ArrayList;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The resume detail view displays the details of a resume.
 */
@Route(value = "resume", layout = MainLayout.class)
@PageTitle("Resume AI - Resume Details")
public class ResumeDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final ResumeStorageService resumeStorageService;
    private final ResumeMatchingService resumeMatchingService;
    
    private UUID resumeId;
    private Resume resume;
    private ResumeAnalysis analysis;
    private boolean fromMatchResults = false;

    public ResumeDetailView(ResumeStorageService resumeStorageService, ResumeMatchingService resumeMatchingService) {
        this.resumeStorageService = resumeStorageService;
        this.resumeMatchingService = resumeMatchingService;
        
        addClassName("resume-detail-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            resumeId = UUID.fromString(parameter);
            
            // Check if we're coming from match results
            fromMatchResults = event.getLocation().getQueryParameters()
                    .getParameters().containsKey("from") && 
                    event.getLocation().getQueryParameters()
                    .getParameters().get("from").contains("match");
            
            // Load resume
            Optional<Resume> optionalResume = resumeStorageService.getResumeById(resumeId);
            
            if (optionalResume.isPresent()) {
                resume = optionalResume.get();
                buildUI();
            } else {
                showNotFound();
            }
        } catch (IllegalArgumentException e) {
            showNotFound();
        }
    }

    private void buildUI() {
        removeAll();
        
        add(createHeaderSection());
        add(createResumeSection());
        add(createActionsSection());
    }

    private void showNotFound() {
        removeAll();
        
        VerticalLayout notFoundLayout = new VerticalLayout();
        notFoundLayout.setAlignItems(Alignment.CENTER);
        notFoundLayout.setWidthFull();
        
        Icon icon = new Icon(VaadinIcon.FILE_SEARCH);
        icon.setSize("100px");
        icon.setColor("var(--lumo-contrast-30pct)");
        
        H2 title = new H2("Resume Not Found");
        
        Paragraph message = new Paragraph("The resume you are looking for does not exist or has been removed.");
        
        Button backButton = new Button("Back to All Resumes", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("resumes")));
        
        notFoundLayout.add(icon, title, message, backButton);
        add(notFoundLayout);
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout section = new VerticalLayout();
        section.setAlignItems(Alignment.CENTER);
        section.setWidthFull();
        
        H1 title = new H1("Resume Details");
        title.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        section.add(title);
        return section;
    }

    private VerticalLayout createResumeSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        section.setWidthFull();
        section.setMaxWidth("900px");
        
        // Header with name and basic info
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.addClassNames(
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM);
        
        // Left side - Name and contact
        VerticalLayout nameSection = new VerticalLayout();
        nameSection.setPadding(false);
        nameSection.setSpacing(false);
        
        H2 name = new H2(resume.getName());
        name.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.TextColor.BODY,
                LumoUtility.Margin.NONE);
        
        HorizontalLayout contactLayout = new HorizontalLayout();
        contactLayout.setSpacing(true);
        
        Span emailIcon = new Span(new Icon(VaadinIcon.ENVELOPE));
        emailIcon.addClassNames(LumoUtility.TextColor.PRIMARY);
        
        Span email = new Span(resume.getEmail());
        email.addClassNames(LumoUtility.TextColor.SECONDARY);
        
        Span phoneIcon = new Span(new Icon(VaadinIcon.PHONE));
        phoneIcon.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.MEDIUM);
        
        Span phone = new Span(resume.getPhoneNumber());
        phone.addClassNames(LumoUtility.TextColor.SECONDARY);
        
        contactLayout.add(emailIcon, email, phoneIcon, phone);
        nameSection.add(name, contactLayout);
        
        // Right side - File info
        VerticalLayout fileInfoSection = new VerticalLayout();
        fileInfoSection.setPadding(false);
        fileInfoSection.setSpacing(false);
        fileInfoSection.setAlignItems(Alignment.END);
        
        // Format date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String uploadDate = resume.getUploadedAt() != null ? 
                resume.getUploadedAt().format(formatter) : "N/A";
        
        Span fileType = new Span(resume.getFileType().toUpperCase());
        fileType.addClassNames(
                LumoUtility.Background.PRIMARY,
                LumoUtility.TextColor.PRIMARY_CONTRAST,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.FontSize.SMALL);
        
        Span fileName = new Span(resume.getOriginalFileName());
        fileName.addClassNames(
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontSize.SMALL,
                LumoUtility.Margin.Top.XSMALL);
        
        Span uploadInfo = new Span("Uploaded: " + uploadDate);
        uploadInfo.addClassNames(
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontSize.XSMALL,
                LumoUtility.Margin.Top.XSMALL);
        
        fileInfoSection.add(fileType, fileName, uploadInfo);
        
        header.add(nameSection, fileInfoSection);
        
        // Content tabs
        Tabs tabs = new Tabs();
        tabs.setWidthFull();
        
        Tab rawTab = new Tab("Raw Content");
        Tab parsedTab = new Tab("Parsed Content");
        Tab analysisTab = new Tab("AI Analysis");
        
        tabs.add(rawTab, parsedTab, analysisTab);
        tabs.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);
        
        // Content area
        Div contentArea = new Div();
        contentArea.setWidthFull();
        contentArea.addClassNames(
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.BorderRadius.MEDIUM);
        
        // Raw content view
        Div rawContent = createRawContentView();
        
        // Parsed content view
        Div parsedContent = createParsedContentView();
        
        // AI Analysis view
        Div analysisContent = createAnalysisView();
        
        // Add all content views to the content area
        contentArea.add(rawContent, parsedContent, analysisContent);
        
        // Initially show raw content only
        rawContent.setVisible(true);
        parsedContent.setVisible(false);
        analysisContent.setVisible(false);
        
        // Set up tab change listener
        tabs.addSelectedChangeListener(event -> {
            // Remove visible flag from all content components
            rawContent.setVisible(false);
            parsedContent.setVisible(false);
            analysisContent.setVisible(false);
            
            // Show the selected content
            Tab selectedTab = tabs.getSelectedTab();
            if (selectedTab.equals(rawTab)) {
                rawContent.setVisible(true);
            } else if (selectedTab.equals(parsedTab)) {
                parsedContent.setVisible(true);
            } else if (selectedTab.equals(analysisTab)) {
                analysisContent.setVisible(true);
            }
        });
        
        // Add all components to the section
        section.add(header, tabs, contentArea);
        
        return section;
    }
    
    /**
     * Creates the raw content view.
     */
    private Div createRawContentView() {
        Div contentDiv = new Div();
        contentDiv.setWidthFull();
        
        // Resume content
        Div textContent = new Div();
        textContent.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM);
        textContent.getStyle().set("font-family", "monospace");
        textContent.getStyle().set("white-space", "pre-wrap");
        textContent.getStyle().set("overflow", "auto");
        textContent.getStyle().set("max-height", "500px");
        textContent.setText(resume.getFullText());
        
        contentDiv.add(textContent);
        return contentDiv;
    }
    
    /**
     * Creates the parsed content view with structured information.
     */
    private Div createParsedContentView() {
        Div contentDiv = new Div();
        contentDiv.setWidthFull();
        
        // Extract sections from resume text
        Map<String, String> sections = extractResumeSections(resume.getFullText());
        
        // Create accordion for sections
        Accordion accordion = new Accordion();
        accordion.setWidthFull();
        
        // Add sections to accordion
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String sectionName = entry.getKey();
            String sectionContent = entry.getValue();
            
            // Create section content
            Div sectionDiv = new Div();
            sectionDiv.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Background.CONTRAST_5);
            sectionDiv.getStyle().set("white-space", "pre-wrap");
            sectionDiv.setText(sectionContent);
            
            // Add to accordion
            accordion.add(sectionName, sectionDiv);
        }
        
        contentDiv.add(accordion);
        return contentDiv;
    }
    
    /**
     * Creates the AI analysis view.
     */
    private Div createAnalysisView() {
        Div contentDiv = new Div();
        contentDiv.setWidthFull();
        
        // Try to get analysis data if available
        if (analysis == null) {
            // Check if we have a match result with analysis
            try {
                // This is a placeholder - we would need to implement this method in ResumeMatchingService
                // For now, we'll just use the default view
                /*
                List<ResumeMatch> matches = resumeMatchingService.getRecentMatchesForResume(resumeId, 1);
                if (!matches.isEmpty() && matches.get(0).getAnalysis() != null) {
                    analysis = matches.get(0).getAnalysis();
                }
                */
            } catch (Exception e) {
                // Ignore errors, we'll use the default view
            }
        }
        
        // If we have structured analysis data, use that
        if (analysis != null) {
            return createStructuredAnalysisView(analysis);
        }
        
        // Enhanced AI analysis section with modern design (default view)
        VerticalLayout analysisLayout = new VerticalLayout();
        analysisLayout.setPadding(true);
        analysisLayout.setSpacing(true);
        analysisLayout.setWidthFull();
        
        // AI Analysis header with icon
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.addClassNames(
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM);
        
        Icon aiIcon = new Icon(VaadinIcon.AUTOMATION);
        aiIcon.setSize("2em");
        aiIcon.setColor("var(--lumo-primary-color)");
        
        H3 title = new H3("AI Resume Analysis");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XLARGE);
        
        headerLayout.add(aiIcon, title);
        
        // Analysis cards section
        VerticalLayout cardsLayout = new VerticalLayout();
        cardsLayout.setPadding(false);
        cardsLayout.setSpacing(true);
        cardsLayout.setWidthFull();
        
        // Skills Analysis Card
        Div skillsCard = createAnalysisCard(
                "Skills Assessment", 
                VaadinIcon.TOOLS, 
                "This resume shows proficiency in several key technical areas. " +
                "The candidate demonstrates experience with programming languages, " +
                "frameworks, and tools relevant to software development roles.",
                "var(--lumo-success-color)");
        
        // Experience Analysis Card
        Div experienceCard = createAnalysisCard(
                "Experience Evaluation", 
                VaadinIcon.WORKPLACE, 
                "The candidate has relevant work experience that aligns with " +
                "typical requirements for this role. Their background shows " +
                "progression and increasing responsibility in related positions.",
                "var(--lumo-primary-color)");
        
        // Education Analysis Card
        Div educationCard = createAnalysisCard(
                "Education Background", 
                VaadinIcon.ACADEMY_CAP, 
                "The educational qualifications appear suitable for the position. " +
                "The candidate has formal training in relevant disciplines that " +
                "provide a foundation for the required job skills.",
                "var(--lumo-contrast-60pct)");
        
        // Match Potential Card
        Div matchCard = createAnalysisCard(
                "Match Potential", 
                VaadinIcon.CHART, 
                "To get a detailed match analysis for a specific job, use the " +
                "'Match with Job' feature to compare this resume with a job description.",
                "var(--lumo-primary-color)");
        
        // Add action button to the match card
        Button matchButton = new Button("Match with Job", new Icon(VaadinIcon.SEARCH));
        matchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        matchButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("match")));
        matchButton.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        
        // Add the button to the match card
        ((VerticalLayout)matchCard.getChildren().findFirst().get()).add(matchButton);
        
        // Add all cards to the layout
        cardsLayout.add(skillsCard, experienceCard, educationCard, matchCard);
        
        // Add all components to the main layout
        analysisLayout.add(headerLayout, cardsLayout);
        
        contentDiv.add(analysisLayout);
        return contentDiv;
    }
    
    /**
     * Creates a structured analysis view based on the ResumeAnalysis object.
     */
    private Div createStructuredAnalysisView(ResumeAnalysis analysis) {
        Div contentDiv = new Div();
        contentDiv.setWidthFull();
        
        // Enhanced AI analysis section with modern design
        VerticalLayout analysisLayout = new VerticalLayout();
        analysisLayout.setPadding(true);
        analysisLayout.setSpacing(true);
        analysisLayout.setWidthFull();
        
        // AI Analysis header with icon
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.addClassNames(
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM);
        
        Icon aiIcon = new Icon(VaadinIcon.AUTOMATION);
        aiIcon.setSize("2em");
        aiIcon.setColor("var(--lumo-primary-color)");
        
        H3 title = new H3("AI Resume Analysis");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XLARGE);
        
        headerLayout.add(aiIcon, title);
        
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
            analysisLayout.add(summarySection);
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
            analysisLayout.add(scoreSection);
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
            
            analysisLayout.add(scoresSection);
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
            
            analysisLayout.add(skillsSection);
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
            
            analysisLayout.add(strengthsSection);
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
            
            analysisLayout.add(improvementSection);
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
            analysisLayout.add(recommendationSection);
        }
        
        // Match with Job button
        Button matchButton = new Button("Match with Job", new Icon(VaadinIcon.SEARCH));
        matchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        matchButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("match")));
        matchButton.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        
        analysisLayout.add(matchButton);
        
        contentDiv.add(analysisLayout);
        return contentDiv;
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
     * Creates an analysis card with icon and content.
     */
    private Div createAnalysisCard(String title, VaadinIcon icon, String content, String accentColor) {
        Div card = new Div();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM);
        card.getStyle().set("border-left", "4px solid " + accentColor);
        card.getStyle().set("border-left-color", accentColor);
        
        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setPadding(false);
        cardContent.setSpacing(false);
        
        // Card header with icon
        HorizontalLayout cardHeader = new HorizontalLayout();
        cardHeader.setAlignItems(Alignment.CENTER);
        cardHeader.setSpacing(true);
        
        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(accentColor);
        cardIcon.setSize("1.2em");
        
        H4 cardTitle = new H4(title);
        cardTitle.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);
        
        cardHeader.add(cardIcon, cardTitle);
        
        // Card content
        Paragraph cardText = new Paragraph(content);
        cardText.addClassNames(
                LumoUtility.TextColor.BODY,
                LumoUtility.FontSize.SMALL,
                LumoUtility.Margin.Top.SMALL,
                LumoUtility.Margin.Bottom.NONE);
        cardText.getStyle().set("line-height", "1.5");
        
        cardContent.add(cardHeader, cardText);
        card.add(cardContent);
        
        return card;
    }
    
    /**
     * Extracts sections from resume text.
     */
    private Map<String, String> extractResumeSections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        
        // Default sections if we can't extract them
        sections.put("Contact Information", resume.getName() + "\n" + resume.getEmail() + "\n" + resume.getPhoneNumber());
        
        // Try to extract common resume sections
        // This is a simple implementation - in a real app, you'd use more sophisticated parsing
        
        // Common section headers
        String[] sectionHeaders = {
                "EDUCATION", "EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT", 
                "SKILLS", "TECHNICAL SKILLS", "PROJECTS", "CERTIFICATIONS",
                "LANGUAGES", "AWARDS", "PUBLICATIONS", "REFERENCES"
        };
        
        // Simple parsing logic - find sections based on common headers
        String[] lines = text.split("\n");
        String currentSection = "General";
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            String upperLine = line.trim().toUpperCase();
            boolean foundSection = false;
            
            // Check if this line is a section header
            for (String header : sectionHeaders) {
                if (upperLine.contains(header) && (upperLine.length() < header.length() + 10)) {
                    // If we were building a previous section, add it to the map
                    if (currentContent.length() > 0) {
                        sections.put(currentSection, currentContent.toString());
                        currentContent = new StringBuilder();
                    }
                    
                    // Start a new section
                    currentSection = line.trim();
                    foundSection = true;
                    break;
                }
            }
            
            // If not a section header, add to current content
            if (!foundSection) {
                currentContent.append(line).append("\n");
            }
        }
        
        // Add the last section
        if (currentContent.length() > 0) {
            sections.put(currentSection, currentContent.toString());
        }
        
        return sections;
    }

    private VerticalLayout createActionsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.setSpacing(true);
        
        // Breadcrumb navigation with enhanced styling
        HorizontalLayout breadcrumbs = new HorizontalLayout();
        breadcrumbs.setWidthFull();
        breadcrumbs.setAlignItems(Alignment.CENTER);
        breadcrumbs.addClassNames(
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Margin.Bottom.MEDIUM,
                LumoUtility.BoxShadow.XSMALL);
        
        // Home link
        Anchor homeLink = new Anchor("", "Home");
        homeLink.addClassNames(LumoUtility.TextColor.SECONDARY);
        
        Icon separatorIcon1 = new Icon(VaadinIcon.ANGLE_RIGHT);
        separatorIcon1.setSize("0.8em");
        separatorIcon1.addClassNames(LumoUtility.TextColor.TERTIARY);
        
        // Create dynamic breadcrumbs based on navigation path
        if (fromMatchResults) {
            // Match link
            Anchor matchLink = new Anchor("match", "Match");
            matchLink.addClassNames(LumoUtility.TextColor.SECONDARY);
            
            Icon separatorIcon2 = new Icon(VaadinIcon.ANGLE_RIGHT);
            separatorIcon2.setSize("0.8em");
            separatorIcon2.addClassNames(LumoUtility.TextColor.TERTIARY);
            
            // Results link
            Span resultsLink = new Span("Match Results");
            resultsLink.addClassNames(LumoUtility.TextColor.SECONDARY);
            
            Icon separatorIcon3 = new Icon(VaadinIcon.ANGLE_RIGHT);
            separatorIcon3.setSize("0.8em");
            separatorIcon3.addClassNames(LumoUtility.TextColor.TERTIARY);
            
            // Current page
            Span currentPage = new Span("Resume Details");
            currentPage.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
            
            breadcrumbs.add(homeLink, separatorIcon1, matchLink, separatorIcon2, 
                    resultsLink, separatorIcon3, currentPage);
        } else {
            // Resumes link
            Anchor resumesLink = new Anchor("resumes", "Resumes");
            resumesLink.addClassNames(LumoUtility.TextColor.SECONDARY);
            
            Icon separatorIcon2 = new Icon(VaadinIcon.ANGLE_RIGHT);
            separatorIcon2.setSize("0.8em");
            separatorIcon2.addClassNames(LumoUtility.TextColor.TERTIARY);
            
            // Current page
            Span currentPage = new Span("Resume Details");
            currentPage.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
            
            breadcrumbs.add(homeLink, separatorIcon1, resumesLink, separatorIcon2, currentPage);
        }
        
        // Action buttons with enhanced styling
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        buttons.setSpacing(true);
        
        // Back button - changes based on where we came from
        Button backButton;
        if (fromMatchResults) {
            backButton = new Button("Back to Match Results", new Icon(VaadinIcon.ARROW_LEFT));
            backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("match")));
            // Add a subtle highlight to indicate this is the recommended action
            backButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else {
            backButton = new Button("Back to All Resumes", new Icon(VaadinIcon.ARROW_LEFT));
            backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("resumes")));
        }
        
        // Match button
        Button matchButton = new Button("Match with Job", new Icon(VaadinIcon.SEARCH));
        matchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        matchButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("match")));
        
        // Delete button
        Button deleteButton = new Button("Delete Resume", new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.addClickListener(e -> deleteResume());
        
        buttons.add(backButton, matchButton, deleteButton);
        
        section.add(breadcrumbs, buttons);
        return section;
    }

    private void deleteResume() {
        try {
            resumeStorageService.deleteResume(resumeId);
            
            // Show notification
            Notification notification = new Notification(
                    "Resume deleted successfully", 
                    3000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.open();
            
            // Navigate back to resumes
            getUI().ifPresent(ui -> ui.navigate("resumes"));
            
        } catch (Exception e) {
            // Show error
            Notification notification = new Notification(
                    "Error deleting resume: " + e.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        }
    }
}
