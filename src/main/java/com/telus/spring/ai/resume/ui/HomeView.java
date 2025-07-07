package com.telus.spring.ai.resume.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * The home view is the default view shown when navigating to the application.
 * It provides an overview of the application's features.
 */
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@PageTitle("Resume AI")
public class HomeView extends VerticalLayout {
    
    public HomeView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        
        add(createHeader());
        add(createFeatureSection());
    }
    
    private VerticalLayout createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.CENTER);
        
        H1 title = new H1("Resume AI");
        title.getStyle().set("margin-top", "0");
        
        Paragraph subtitle = new Paragraph("Intelligent Resume Analysis and Matching");
        subtitle.getStyle().set("font-size", "1.2em");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        header.add(title, subtitle);
        return header;
    }
    
    private VerticalLayout createFeatureSection() {
        VerticalLayout features = new VerticalLayout();
        features.setAlignItems(Alignment.CENTER);
        features.setJustifyContentMode(JustifyContentMode.CENTER);
        features.setMaxWidth("800px");
        
        H2 featuresTitle = new H2("Features");
        
        Paragraph chatFeature = new Paragraph("Chat with AI about resumes and job descriptions");
        Anchor chatLink = new Anchor("chat", "Try the Chat Interface");
        chatLink.getStyle().set("margin-top", "1em");
        
        Paragraph matchFeature = new Paragraph("Match resumes to job descriptions");
        Anchor matchLink = new Anchor("match", "Try the Job Matching Tool");
        matchLink.getStyle().set("margin-top", "1em");
        
        Paragraph analyzeFeature = new Paragraph("Get detailed analysis of resumes");
        Paragraph compareFeature = new Paragraph("Compare multiple candidates");
        
        features.add(
            featuresTitle,
            chatFeature,
            chatLink,
            matchFeature,
            matchLink,
            analyzeFeature,
            compareFeature
        );
        
        return features;
    }
}
