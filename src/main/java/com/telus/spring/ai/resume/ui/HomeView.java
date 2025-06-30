package com.telus.spring.ai.resume.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The home view is the landing page of the application.
 * Simplified to focus on vector store synchronization and matching.
 */
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@PageTitle("Resume AI - Home")
public class HomeView extends VerticalLayout {

    public HomeView() {
        addClassName("home-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        
        add(createHeroSection());
        add(createFeaturesSection());
        add(createActionSection());
    }

    private VerticalLayout createHeroSection() {
        VerticalLayout section = new VerticalLayout();
        section.setAlignItems(Alignment.CENTER);
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        
        H1 title = new H1("Resume AI");
        title.addClassNames(
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.TextAlignment.CENTER);
        
        Paragraph subtitle = new Paragraph("Intelligent Resume Matching with Vector Search and AI");
        subtitle.addClassNames(
                LumoUtility.FontSize.XLARGE,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.TextAlignment.CENTER);
        
        Icon heroIcon = VaadinIcon.FILE_SEARCH.create();
        heroIcon.setSize("120px");
        heroIcon.setColor("#1676f3");
        
        section.add(title, subtitle, heroIcon);
        return section;
    }

    private HorizontalLayout createFeaturesSection() {
        HorizontalLayout section = new HorizontalLayout();
        section.setWidthFull();
        section.setJustifyContentMode(JustifyContentMode.CENTER);
        section.setSpacing(true);
        section.setPadding(true);
        
        // Feature 1 - Changed from Upload to Vector Store
        VerticalLayout feature1 = createFeatureCard(
                VaadinIcon.DATABASE,
                "Vector Database",
                "Store resume embeddings in a vector database for efficient semantic search and matching."
        );
        
        // Feature 2
        VerticalLayout feature2 = createFeatureCard(
                VaadinIcon.SEARCH,
                "Match with Jobs",
                "Find the best matching resumes for a job description using semantic search."
        );
        
        // Feature 3 - Changed to Sync
        VerticalLayout feature3 = createFeatureCard(
                VaadinIcon.REFRESH,
                "Automatic Sync",
                "Automatically synchronize the database with the vector store to ensure data consistency."
        );
        
        section.add(feature1, feature2, feature3);
        return section;
    }

    private VerticalLayout createFeatureCard(VaadinIcon icon, String title, String description) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        card.setAlignItems(Alignment.CENTER);
        card.setWidth("300px");
        card.setHeight("250px");
        
        Icon featureIcon = icon.create();
        featureIcon.setSize("50px");
        featureIcon.setColor("#1676f3");
        
        H2 featureTitle = new H2(title);
        featureTitle.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.TextAlignment.CENTER);
        
        Paragraph featureDescription = new Paragraph(description);
        featureDescription.addClassNames(
                LumoUtility.TextAlignment.CENTER,
                LumoUtility.TextColor.SECONDARY);
        
        card.add(featureIcon, featureTitle, featureDescription);
        return card;
    }

    private HorizontalLayout createActionSection() {
        HorizontalLayout section = new HorizontalLayout();
        section.setWidthFull();
        section.setJustifyContentMode(JustifyContentMode.CENTER);
        section.setSpacing(true);
        section.setPadding(true);
        
        // Match button - now primary
        Button matchButton = new Button("Match Resumes", new Icon(VaadinIcon.SEARCH));
        matchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        matchButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("match")));
        
        // View all button
        Button viewAllButton = new Button("View All Resumes", new Icon(VaadinIcon.LIST));
        viewAllButton.addThemeVariants(ButtonVariant.LUMO_LARGE);
        viewAllButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("resumes")));
        
        // Sync button - added to replace upload
        Button syncButton = new Button("Vector Store Sync", new Icon(VaadinIcon.DATABASE));
        syncButton.addThemeVariants(ButtonVariant.LUMO_LARGE);
        syncButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("sync")));
        
        section.add(matchButton, viewAllButton, syncButton);
        return section;
    }
}
