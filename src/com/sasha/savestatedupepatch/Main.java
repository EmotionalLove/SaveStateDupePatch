package com.sasha.savestatedupepatch;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Minecraft's chunk format contains a bug that causes the chunk not to save when
 * there are items with overloaded NBT (like books) in a container together.
 *
 * This can be exploited to duplicate the contents (including containers and placed blocks) of an entire chunk
 *
 * This plugin attempts to fix this by reverting awkwardly large books to a reasonable size.
 *
 * @author Sasha Stevens
 */

public class Main extends JavaPlugin implements Listener {


    @Override
    public void onEnable() {
        System.out.println("SaveStateDupePatch is enabling...");
        this.getServer().getPluginManager().registerEvents(this, this);
        System.out.println("SaveStateDupePatch is enabled!");
    }

    @Override
    public void onDisable() {
        System.out.println("SaveStateDupePatch is disabled!");
    }

    /**
     * Intercept new books that are written that have strings bigger than 28000 bytes
     * Books that are too big will be overwriteen with "Previously saved book content was too big!" on each page
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookWrite(PlayerEditBookEvent e) {
        if (getPayloadBytes(e.getNewBookMeta().getPages()) > 28000) {
            BookMeta newestMeta = e.getNewBookMeta();
            for (int i = 0; i < e.getNewBookMeta().getPages().size(); i++) {
                newestMeta.setPage(i + 1, "Previously saved book content was too big!");
            }
            e.setNewBookMeta(newestMeta);
        }
    }

    /**
     * Invoked when a player opens their inventory or a container
     * Will revert any overloaded books upon opening that container
     */
    @EventHandler
    public void onContainerOpened(InventoryOpenEvent e) {
        for (ItemStack item : e.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() == Material.BOOK_AND_QUILL || item.getType() == Material.WRITTEN_BOOK) {
                BookMeta meta = (BookMeta) item.getItemMeta();
                BookMeta newestMeta = meta;
                for (int i = 0; i < meta.getPages().size(); i++) {
                    newestMeta.setPage(i + 1, "Previously saved book content was too big!");
                }
                item.setItemMeta(newestMeta);
            }
        }
    }

    /**
     * Invoked when a hopper or a player moves an item in their inventory or across inventories
     * Will revert any overloaded books upon movement of that book.
     */
    @EventHandler
    public void onBookMove(InventoryMoveItemEvent e) { // called when a hopper transports an item
        if (e.getItem().getType() == Material.BOOK_AND_QUILL || e.getItem().getType() == Material.WRITTEN_BOOK) {
            BookMeta meta = (BookMeta) e.getItem().getItemMeta();
            for (int i = 0; i < meta.getPages().size(); i++) {
                meta.setPage(i + 1, "Previously saved book content was too big!");
            }
            e.getItem().setItemMeta(meta);
        }
    }

    /**
     * Gets the total bytes of a book
     * @param bookPages the list of strings that each page contains
     * @return the length in bytes
     */
    private int getPayloadBytes(List<String> bookPages) {
        StringBuilder builder = new StringBuilder();
        bookPages.forEach(builder::append); // append content to strbuilder
        return builder.toString().getBytes().length;
    }

}
