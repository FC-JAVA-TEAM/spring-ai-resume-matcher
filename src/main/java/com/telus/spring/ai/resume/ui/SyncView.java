package com.telus.spring.ai.resume.ui;

import com.telus.spring.ai.resume.model.SyncResult;
import com.telus.spring.ai.resume.scheduler.VectorStoreSyncScheduler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The sync view allows administrators to synchronize the vector store with the database.
 * Enhanced to use the VectorStoreSyncScheduler for better status tracking.
 */
@Route(value = "sync", layout = MainLayout.class)
@PageTitle("Resume AI - Vector Store Sync")
public class SyncView extends VerticalLayout {

    private final VectorStoreSyncScheduler syncScheduler;
    
    private Button syncButton;
    private ProgressBar progressBar;
    private VerticalLayout statusLayout;
    private VerticalLayout resultLayout;
    
    private Paragraph lastSyncValue;
    private Paragraph nextSyncValue;
    private Span syncStatusValue;
    
    // For polling status updates
    private static final int POLL_INTERVAL = 2000; // 2 seconds
    private boolean polling = false;

    public SyncView(VectorStoreSyncScheduler syncScheduler) {
        this.syncScheduler = syncScheduler;
        
        addClassName("sync-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        
        add(createHeaderSection());
        add(createSyncSection());
        add(createStatusSection());
        add(createResultSection());
        
        // Initial status update
        updateStatus();
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout section = new VerticalLayout();
        section.setAlignItems(Alignment.CENTER);
        section.setWidthFull();
        
        H1 title = new H1("Vector Store Synchronization");
        title.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        Paragraph description = new Paragraph(
                "Synchronize the vector store with the database to ensure all resumes are properly indexed for semantic search.");
        description.addClassNames(LumoUtility.TextAlignment.CENTER);
        
        section.add(title, description);
        return section;
    }

    private VerticalLayout createSyncSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        section.setWidthFull();
        section.setMaxWidth("800px");
        
        H3 sectionTitle = new H3("Manual Synchronization");
        
        Paragraph info = new Paragraph(
                "The vector store is automatically synchronized with the database daily at 2 AM. " +
                "You can also manually trigger a synchronization if needed.");
        
        // Progress bar
        progressBar = new ProgressBar();
        progressBar.setWidthFull();
        progressBar.setVisible(false);
        
        // Sync button
        syncButton = new Button("Synchronize Now", new Icon(VaadinIcon.REFRESH));
        syncButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        syncButton.setWidthFull();
        syncButton.addClickListener(e -> synchronize());
        
        section.add(sectionTitle, info, progressBar, syncButton);
        return section;
    }

    private VerticalLayout createStatusSection() {
        statusLayout = new VerticalLayout();
        statusLayout.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        statusLayout.setWidthFull();
        statusLayout.setMaxWidth("800px");
        
        H3 sectionTitle = new H3("Synchronization Status");
        
        // Create status info
        VerticalLayout statusInfo = new VerticalLayout();
        statusInfo.setPadding(false);
        statusInfo.setSpacing(true);
        
        // Last sync time
        HorizontalLayout lastSyncLayout = new HorizontalLayout();
        lastSyncLayout.setWidthFull();
        
        Paragraph lastSyncLabel = new Paragraph("Last Synchronization:");
        lastSyncLabel.getStyle().set("font-weight", "bold");
        lastSyncLabel.setWidth("200px");
        
        lastSyncValue = new Paragraph("N/A");
        
        lastSyncLayout.add(lastSyncLabel, lastSyncValue);
        
        // Next scheduled sync
        HorizontalLayout nextSyncLayout = new HorizontalLayout();
        nextSyncLayout.setWidthFull();
        
        Paragraph nextSyncLabel = new Paragraph("Next Scheduled Sync:");
        nextSyncLabel.getStyle().set("font-weight", "bold");
        nextSyncLabel.setWidth("200px");
        
        // Calculate next sync time (2 AM tomorrow)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSync = now.toLocalDate().plusDays(1).atTime(2, 0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        nextSyncValue = new Paragraph(nextSync.format(formatter));
        
        nextSyncLayout.add(nextSyncLabel, nextSyncValue);
        
        // Sync status
        HorizontalLayout syncStatusLayout = new HorizontalLayout();
        syncStatusLayout.setWidthFull();
        
        Paragraph syncStatusLabel = new Paragraph("Current Status:");
        syncStatusLabel.getStyle().set("font-weight", "bold");
        syncStatusLabel.setWidth("200px");
        
        syncStatusValue = new Span("Idle");
        syncStatusValue.getElement().getThemeList().add("badge success");
        
        syncStatusLayout.add(syncStatusLabel, syncStatusValue);
        
        statusInfo.add(lastSyncLayout, nextSyncLayout, syncStatusLayout);
        
        statusLayout.add(sectionTitle, statusInfo);
        return statusLayout;
    }

    private VerticalLayout createResultSection() {
        resultLayout = new VerticalLayout();
        resultLayout.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        resultLayout.setWidthFull();
        resultLayout.setMaxWidth("800px");
        resultLayout.setVisible(false);
        
        return resultLayout;
    }

    private void synchronize() {
        try {
            // Check if sync is already in progress
            if (syncScheduler.isSyncInProgress()) {
                Notification notification = new Notification(
                        "Synchronization already in progress", 
                        3000, 
                        Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                notification.open();
                return;
            }
            
            // Show loading indicator
            progressBar.setVisible(true);
            syncButton.setEnabled(false);
            syncButton.setText("Synchronizing...");
            
            // Update status
            updateStatus();
            
            // Start polling for status updates
            startPolling();
            
            // Trigger synchronization
            SyncResult result = syncScheduler.triggerSync();
            
            if (result == null) {
                // Sync failed to start
                stopPolling();
                progressBar.setVisible(false);
                syncButton.setEnabled(true);
                syncButton.setText("Synchronize Now");
                
                Notification notification = new Notification(
                        "Failed to start synchronization", 
                        3000, 
                        Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } else {
                // Sync completed successfully
                stopPolling();
                
                // Show result
                showResult(result);
                
                // Reset UI
                progressBar.setVisible(false);
                syncButton.setEnabled(true);
                syncButton.setText("Synchronize Now");
                
                // Update status
                updateStatus();
                
                // Show success notification
                Notification notification = new Notification(
                        "Synchronization completed successfully", 
                        3000, 
                        Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                notification.open();
            }
            
        } catch (Exception e) {
            // Stop polling
            stopPolling();
            
            // Reset UI
            progressBar.setVisible(false);
            syncButton.setEnabled(true);
            syncButton.setText("Synchronize Now");
            
            // Update status
            updateStatus();
            
            // Show error notification
            Notification notification = new Notification(
                    "Error during synchronization: " + e.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        }
    }

    private void showResult(SyncResult result) {
        resultLayout.removeAll();
        
        H3 sectionTitle = new H3("Synchronization Results");
        
        Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
        icon.setSize("50px");
        icon.setColor("var(--lumo-success-color)");
        
        // Create result info
        VerticalLayout resultInfo = new VerticalLayout();
        resultInfo.setPadding(false);
        resultInfo.setSpacing(true);
        
        // Total processed
        HorizontalLayout totalLayout = new HorizontalLayout();
        totalLayout.setWidthFull();
        
        Paragraph totalLabel = new Paragraph("Total Changes:");
        totalLabel.getStyle().set("font-weight", "bold");
        totalLabel.setWidth("250px");
        
        int total = result.getDuplicatesRemoved() + result.getMissingAdded() + result.getOrphansRemoved();
        Paragraph totalValue = new Paragraph(String.valueOf(total));
        
        totalLayout.add(totalLabel, totalValue);
        
        // Added
        HorizontalLayout addedLayout = new HorizontalLayout();
        addedLayout.setWidthFull();
        
        Paragraph addedLabel = new Paragraph("Resumes Added to Vector Store:");
        addedLabel.getStyle().set("font-weight", "bold");
        addedLabel.setWidth("250px");
        
        Paragraph addedValue = new Paragraph(String.valueOf(result.getMissingAdded()));
        
        addedLayout.add(addedLabel, addedValue);
        
        // Duplicates removed
        HorizontalLayout duplicatesLayout = new HorizontalLayout();
        duplicatesLayout.setWidthFull();
        
        Paragraph duplicatesLabel = new Paragraph("Duplicate Entries Removed:");
        duplicatesLabel.getStyle().set("font-weight", "bold");
        duplicatesLabel.setWidth("250px");
        
        Paragraph duplicatesValue = new Paragraph(String.valueOf(result.getDuplicatesRemoved()));
        
        duplicatesLayout.add(duplicatesLabel, duplicatesValue);
        
        // Orphans removed
        HorizontalLayout orphansLayout = new HorizontalLayout();
        orphansLayout.setWidthFull();
        
        Paragraph orphansLabel = new Paragraph("Orphaned Entries Removed:");
        orphansLabel.getStyle().set("font-weight", "bold");
        orphansLabel.setWidth("250px");
        
        Paragraph orphansValue = new Paragraph(String.valueOf(result.getOrphansRemoved()));
        
        orphansLayout.add(orphansLabel, orphansValue);
        
        resultInfo.add(totalLayout, addedLayout, duplicatesLayout, orphansLayout);
        
        resultLayout.add(sectionTitle, icon, resultInfo);
        resultLayout.setAlignItems(Alignment.CENTER);
        resultLayout.setVisible(true);
    }
    
    private void updateStatus() {
        // Update last sync time
        lastSyncValue.setText(syncScheduler.getLastSyncTime());
        
        // Update sync status
        boolean inProgress = syncScheduler.isSyncInProgress();
        if (inProgress) {
            syncStatusValue.setText("In Progress");
            syncStatusValue.getElement().getThemeList().remove("success");
            syncStatusValue.getElement().getThemeList().add("primary");
            
            // Disable sync button
            syncButton.setEnabled(false);
            syncButton.setText("Synchronizing...");
            progressBar.setVisible(true);
        } else {
            syncStatusValue.setText("Idle");
            syncStatusValue.getElement().getThemeList().remove("primary");
            syncStatusValue.getElement().getThemeList().add("success");
            
            // Enable sync button
            syncButton.setEnabled(true);
            syncButton.setText("Synchronize Now");
            progressBar.setVisible(false);
            
            // Show last result if available
            SyncResult lastResult = syncScheduler.getLastSyncResult();
            if (lastResult != null) {
                showResult(lastResult);
            }
        }
    }
    
    private void startPolling() {
        if (!polling) {
            polling = true;
            UI ui = UI.getCurrent();
            
            new Thread(() -> {
                try {
                    while (polling && syncScheduler.isSyncInProgress()) {
                        Thread.sleep(POLL_INTERVAL);
                        
                        ui.access(() -> {
                            updateStatus();
                        });
                    }
                    
                    // Final update when polling stops
                    ui.access(() -> {
                        updateStatus();
                    });
                } catch (InterruptedException e) {
                    // Thread interrupted, stop polling
                    polling = false;
                }
            }).start();
        }
    }
    
    private void stopPolling() {
        polling = false;
    }
}
