package com.telus.spring.ai.resume.ui;

import com.telus.spring.ai.resume.service.ui.MatchView;
import com.telus.spring.ai.resume.service.ui.ResumesView;
import com.telus.spring.ai.resume.service.ui.SyncView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The main layout for the application.
 * Contains the navigation drawer and header.
 */
public class MainLayout extends AppLayout {
    
    private H1 viewTitle;
    
    public MainLayout() {
        createHeader();
        createDrawer();
    }
    
    private void createHeader() {
        // Logo icon
        Icon logo = VaadinIcon.FILE_TEXT.create();
        logo.setSize("44px");
        logo.setColor("#1676f3");
        
        // App name
        H1 appName = new H1("Resume AI");
        appName.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE);
        
        // View title
        viewTitle = new H1();
        viewTitle.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.NONE
        );
        
        // Drawer toggle
        DrawerToggle toggle = new DrawerToggle();
        
        // Add components to header
        addToNavbar(true, toggle, logo, appName);
    }
    
    private void createDrawer() {
        // Navigation tabs
        Tabs tabs = new Tabs();
        tabs.add(
            createTab(VaadinIcon.HOME, "Home", HomeView.class),
            createTab(VaadinIcon.CHAT, "Chat", ChatView.class),
            createTab(VaadinIcon.SEARCH, "Match Job", MatchNewView.class),
            createTab(VaadinIcon.SEARCH, "Match Resumes", MatchView.class),
            createTab(VaadinIcon.LIST, "All Resumes", ResumesView.class),
            createTab(VaadinIcon.DATABASE, "Vector Store Sync", SyncView.class)
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        
        addToDrawer(tabs);
    }
    
    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> viewClass) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
            .set("margin-inline-end", "var(--lumo-space-m)")
            .set("margin-inline-start", "var(--lumo-space-xs)")
            .set("padding", "var(--lumo-space-xs)");
        
        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(viewClass);
        link.setTabIndex(-1);
        
        return new Tab(link);
    }
    
    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        
        // Set view title based on the PageTitle annotation
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        if (title != null) {
            viewTitle.setText(title.value());
        }
    }
}
