package com.telus.spring.ai.resume.service.ui;

import com.telus.spring.ai.resume.ui.MainLayout;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.service.ResumeStorageService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
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
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * The resumes view displays a list of all resumes in the system.
 */
@Route(value = "resumes", layout = MainLayout.class)
@PageTitle("Resume AI - All Resumes")
public class ResumesView extends VerticalLayout {

    private final ResumeStorageService resumeStorageService;
    
    private Grid<Resume> grid;
    private Button refreshButton;

    public ResumesView(ResumeStorageService resumeStorageService) {
        this.resumeStorageService = resumeStorageService;
        
        addClassName("resumes-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        
        add(createHeaderSection());
        add(createGridSection());
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout section = new VerticalLayout();
        section.setAlignItems(Alignment.CENTER);
        section.setWidthFull();
        
        H1 title = new H1("All Resumes");
        title.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        Paragraph description = new Paragraph(
                "View, search, and manage all resumes in the system.");
        description.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        section.add(title, description);
        return section;
    }

    private VerticalLayout createGridSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        section.setWidthFull();
        section.setMaxWidth("1200px");
        section.setHeight("700px");
        
        // Create toolbar
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.getStyle().set("justify-content", "space-between");
        
        // Left side of toolbar - view options
        HorizontalLayout viewOptions = new HorizontalLayout();
        viewOptions.setSpacing(true);
        
        Button gridViewButton = new Button(new Icon(VaadinIcon.GRID_SMALL));
        gridViewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        gridViewButton.setTooltipText("Grid View");
        
        Button cardViewButton = new Button(new Icon(VaadinIcon.GRID_BIG));
        cardViewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_PRIMARY);
        cardViewButton.setTooltipText("Card View");
        
        viewOptions.add(gridViewButton, cardViewButton);
        
        // Right side of toolbar - actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        
        // Refresh button
        refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> refreshGrid());
        
        // Upload button
        Button uploadButton = new Button("Upload New Resume", new Icon(VaadinIcon.UPLOAD));
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        uploadButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("upload")));
        
        actions.add(refreshButton, uploadButton);
        
        toolbar.add(viewOptions, actions);
        
        // Create a container for resume cards
        VerticalLayout cardsContainer = new VerticalLayout();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        cardsContainer.setWidthFull();
        cardsContainer.setHeight("600px");
        cardsContainer.getStyle().set("overflow-y", "auto");
        
        // We'll populate this container in the refreshGrid method
        
        section.add(toolbar, cardsContainer);
        
        // Load initial data
        refreshGrid();
        
        return section;
    }
    
    /**
     * Creates a card component for a resume.
     */
    private HorizontalLayout createResumeCard(Resume resume) {
        // Main card container
        HorizontalLayout card = new HorizontalLayout();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM);
        card.setWidthFull();
        card.setSpacing(true);
        card.getStyle().set("transition", "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out");
        card.getStyle().set("cursor", "pointer");
        card.getStyle().set("overflow", "hidden");
        
        // Hover effect
        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("transform", "translateY(-3px)");
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("transform", "translateY(0)");
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        });
        
        // Click to view resume
        card.addClickListener(e -> viewResume(resume.getId()));
        
        // Left side - Resume info
        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setPadding(false);
        infoSection.setSpacing(false);
        infoSection.setWidth("70%");
        
        // Name and contact info with icons
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setSpacing(true);
        nameLayout.setPadding(false);
        nameLayout.setAlignItems(Alignment.CENTER);
        nameLayout.setMargin(false);
        
        Icon personIcon = new Icon(VaadinIcon.USER);
        personIcon.setColor("var(--lumo-primary-color)");
        personIcon.setSize("1.2em");
        
        H3 name = new H3(resume.getName());
        name.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.LARGE);
        
        nameLayout.add(personIcon, name);
        
        // Contact info with icons
        HorizontalLayout contactInfo = new HorizontalLayout();
        contactInfo.setSpacing(true);
        contactInfo.setPadding(false);
        contactInfo.setMargin(false);
        contactInfo.setAlignItems(Alignment.CENTER);
        
        Icon emailIcon = new Icon(VaadinIcon.ENVELOPE);
        emailIcon.setColor("var(--lumo-contrast-70pct)");
        emailIcon.setSize("0.9em");
        
        Span email = new Span(resume.getEmail());
        email.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        
        Icon phoneIcon = new Icon(VaadinIcon.PHONE);
        phoneIcon.setColor("var(--lumo-contrast-70pct)");
        phoneIcon.setSize("0.9em");
        phoneIcon.getStyle().set("margin-left", "1em");
        
        Span phone = new Span(resume.getPhoneNumber());
        phone.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        
        contactInfo.add(emailIcon, email, phoneIcon, phone);
        
        // File info with enhanced styling
        HorizontalLayout fileInfo = new HorizontalLayout();
        fileInfo.setSpacing(true);
        fileInfo.setPadding(false);
        fileInfo.setMargin(false);
        fileInfo.setAlignItems(Alignment.CENTER);
        fileInfo.addClassNames(LumoUtility.Margin.Top.SMALL);
        
        // Format date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String uploadDate = resume.getUploadedAt() != null ? 
                resume.getUploadedAt().format(formatter) : "";
        
        // File type badge
        String fileTypeStr = resume.getFileType().toUpperCase();
        String fileTypeColor = getFileTypeColor(fileTypeStr);
        
        HorizontalLayout fileTypeBadge = new HorizontalLayout();
        fileTypeBadge.setSpacing(false);
        fileTypeBadge.setPadding(false);
        fileTypeBadge.setAlignItems(Alignment.CENTER);
        fileTypeBadge.addClassNames(
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.XSMALL);
        fileTypeBadge.getStyle().set("background-color", fileTypeColor + "20"); // 20% opacity
        
        Icon fileIcon = getFileTypeIcon(fileTypeStr);
        fileIcon.setColor(fileTypeColor);
        fileIcon.setSize("0.9em");
        
        Span fileType = new Span(fileTypeStr);
        fileType.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.MEDIUM);
        fileType.getStyle().set("color", fileTypeColor);
        fileType.getStyle().set("margin-left", "4px");
        
        fileTypeBadge.add(fileIcon, fileType);
        
        // Upload info with icon
        HorizontalLayout uploadInfoLayout = new HorizontalLayout();
        uploadInfoLayout.setSpacing(false);
        uploadInfoLayout.setPadding(false);
        uploadInfoLayout.setAlignItems(Alignment.CENTER);
        uploadInfoLayout.getStyle().set("margin-left", "8px");
        
        Icon calendarIcon = new Icon(VaadinIcon.CALENDAR);
        calendarIcon.setColor("var(--lumo-contrast-50pct)");
        calendarIcon.setSize("0.9em");
        
        Span uploadInfo = new Span(uploadDate);
        uploadInfo.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.XSMALL);
        uploadInfo.getStyle().set("margin-left", "4px");
        
        uploadInfoLayout.add(calendarIcon, uploadInfo);
        
        fileInfo.add(fileTypeBadge, uploadInfoLayout);
        
        // Resume content preview with enhanced styling
        Div contentContainer = new Div();
        contentContainer.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL,
                LumoUtility.Margin.Top.MEDIUM);
        
        Paragraph contentPreview = new Paragraph(getContentPreview(resume.getFullText()));
        contentPreview.addClassNames(
                LumoUtility.TextColor.BODY,
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.SMALL);
        contentPreview.getStyle().set("display", "-webkit-box");
        contentPreview.getStyle().set("-webkit-line-clamp", "3");
        contentPreview.getStyle().set("-webkit-box-orient", "vertical");
        contentPreview.getStyle().set("overflow", "hidden");
        contentPreview.getStyle().set("line-height", "1.4");
        
        contentContainer.add(contentPreview);
        
        infoSection.add(nameLayout, contactInfo, fileInfo, contentContainer);
        
        // Right side - Actions
        VerticalLayout actionsSection = new VerticalLayout();
        actionsSection.setAlignItems(Alignment.END);
        actionsSection.getStyle().set("justify-content", "space-between");
        actionsSection.setWidth("30%");
        actionsSection.setHeight("100%");
        
        // File name with icon
        HorizontalLayout fileNameLayout = new HorizontalLayout();
        fileNameLayout.setSpacing(false);
        fileNameLayout.setPadding(false);
        fileNameLayout.setAlignItems(Alignment.CENTER);
        fileNameLayout.getStyle().set("justify-content", "flex-end");
        fileNameLayout.setWidthFull();
        
        Icon fileNameIcon = new Icon(VaadinIcon.FILE_TEXT);
        fileNameIcon.setColor("var(--lumo-contrast-50pct)");
        fileNameIcon.setSize("0.9em");
        
        Span fileName = new Span(resume.getOriginalFileName());
        fileName.addClassNames(
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontSize.XSMALL);
        fileName.getStyle().set("text-align", "right");
        fileName.getStyle().set("word-break", "break-word");
        fileName.getStyle().set("margin-left", "4px");
        
        fileNameLayout.add(fileNameIcon, fileName);
        
        // Action buttons
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setSpacing(true);
        
        Button viewButton = new Button("View Details", new Icon(VaadinIcon.EYE));
        viewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewButton.addClickListener(e -> {
            // Prevent card click from triggering
            e.getSource().getElement().executeJs("event.stopPropagation();");
            viewResume(resume.getId());
        });
        
        Button matchButton = new Button("Match", new Icon(VaadinIcon.SEARCH));
        matchButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        matchButton.addClickListener(e -> {
            // Prevent card click from triggering
            e.getSource().getElement().executeJs("event.stopPropagation();");
            getUI().ifPresent(ui -> ui.navigate("match"));
        });
        
        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        deleteButton.addClickListener(e -> {
            // Prevent card click from triggering
            e.getSource().getElement().executeJs("event.stopPropagation();");
            deleteResume(resume.getId());
        });
        
        actionButtons.add(matchButton, viewButton, deleteButton);
        
        actionsSection.add(fileNameLayout, actionButtons);
        
        // Add sections to card
        card.add(infoSection, actionsSection);
        
        return card;
    }
    
    /**
     * Gets the appropriate icon for a file type.
     */
    private Icon getFileTypeIcon(String fileType) {
        switch (fileType) {
            case "PDF":
                return new Icon(VaadinIcon.FILE_TEXT_O);
            case "DOCX":
            case "DOC":
                return new Icon(VaadinIcon.FILE_TEXT);
            case "TXT":
                return new Icon(VaadinIcon.FILE);
            default:
                return new Icon(VaadinIcon.FILE_O);
        }
    }
    
    /**
     * Gets the appropriate color for a file type.
     */
    private String getFileTypeColor(String fileType) {
        switch (fileType) {
            case "PDF":
                return "var(--lumo-error-color)";
            case "DOCX":
            case "DOC":
                return "var(--lumo-primary-color)";
            case "TXT":
                return "var(--lumo-success-color)";
            default:
                return "var(--lumo-contrast-60pct)";
        }
    }
    
    /**
     * Gets a preview of the resume content.
     */
    private String getContentPreview(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return "No content available";
        }
        
        // Clean up the text
        String cleanText = fullText.replaceAll("\\s+", " ").trim();
        
        // Return a preview (first 200 characters)
        int previewLength = Math.min(cleanText.length(), 200);
        String preview = cleanText.substring(0, previewLength);
        
        return preview + (cleanText.length() > previewLength ? "..." : "");
    }

    private DataProvider<Resume, Void> createDataProvider() {
        return new CallbackDataProvider<>(
                query -> {
                    // Create pageable
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;
                    
                    // Get sort info
                    Sort sort = Sort.by(Sort.Direction.DESC, "uploadedAt");
                    
                    // Create pageable
                    Pageable pageable = PageRequest.of(page, limit, sort);
                    
                    // Get data
                    Page<Resume> resumePage = resumeStorageService.getAllResumes(pageable);
                    
                    return resumePage.getContent().stream();
                },
                query -> {
                    // Get total count
                    Pageable pageable = PageRequest.of(0, 1);
                    Page<Resume> resumePage = resumeStorageService.getAllResumes(pageable);
                    return (int) resumePage.getTotalElements();
                }
        );
    }

    private void refreshGrid() {
        try {
            // Get the cards container - safely access the component
            VerticalLayout gridSection = (VerticalLayout) getComponentAt(1);
            if (gridSection != null && gridSection.getComponentCount() >= 2) {
                VerticalLayout cardsContainer = (VerticalLayout) gridSection.getComponentAt(1);
                cardsContainer.removeAll();
                
                // Get data from the service
                Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "uploadedAt"));
                Page<Resume> resumePage = resumeStorageService.getAllResumes(pageable);
                
                // Add cards for each resume
                for (Resume resume : resumePage.getContent()) {
                    cardsContainer.add(createResumeCard(resume));
                }
            } else {
                // Log error or handle the case where the component structure is not as expected
                System.err.println("Component structure is not as expected in ResumesView");
            }
        } catch (Exception e) {
            // Log the error
            System.err.println("Error refreshing grid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewResume(UUID id) {
        getUI().ifPresent(ui -> ui.navigate("resume/" + id));
    }

    private void deleteResume(UUID id) {
        try {
            resumeStorageService.deleteResume(id);
            
            // Refresh grid
            refreshGrid();
            
            // Show notification
            Notification notification = new Notification(
                    "Resume deleted successfully", 
                    3000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.open();
            
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
