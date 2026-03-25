package org.hascoapi.tests;

/**
 * Utility class to manually clean up test data.
 * Run this before executing tests if you need a fresh start.
 */
public class CleanupTestData {

    public static void main(String[] args) {
        System.out.println("\n========== CLEANING UP TEST DATA ==========");

        // Delete linked elements
        try {
            System.out.println("\n1. Deleting Linked SOC Elements...");
            LinkedSOCElementsSetup.deleteLinkedElements();
            System.out.println("✓ Linked elements deleted");
        } catch (Exception e) {
            System.err.println("⚠ Failed to delete linked elements: " + e.getMessage());
        }

        // Delete orphan elements
        try {
            System.out.println("\n2. Deleting Orphan SOC Elements...");
            OrphanSOCElementsSetup.deleteOrphanElements();
            System.out.println("✓ Orphan elements deleted");
        } catch (Exception e) {
            System.err.println("⚠ Failed to delete orphan elements: " + e.getMessage());
        }

        System.out.println("\n========== CLEANUP COMPLETE ==========");
        System.out.println("You can now run the tests with clean data.");
    }
}

