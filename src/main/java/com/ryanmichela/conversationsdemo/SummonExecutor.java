package com.ryanmichela.conversationsdemo;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.*;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SummonExecutor implements CommandExecutor {
    private Plugin plugin;
    private ConversationFactory conversationFactory;

    public SummonExecutor(Plugin plugin) {
        this.plugin = plugin;
        this.conversationFactory = new ConversationFactory()
                .withModality(true)
                .withPrefix(new PluginNameConversationPrefix(plugin))
                .withFirstPrompt(new WhichMobPrompt());
    }
    
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Conversable) {
            conversationFactory.buildConversation((Conversable)commandSender).begin();
            return true;
        } else {
            return false;
        }
    }

    private class WhichMobPrompt extends FixedSetPrompt {
        public WhichMobPrompt() {
            super(CreatureType.COW.getName(),
                  CreatureType.CHICKEN.getName(),
                  CreatureType.CREEPER.getName(),
                  CreatureType.VILLAGER.getName());
        }

        public String getPromptText(ConversationContext context) {
            return "What would you like to summon? " + formatFixedSet();
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String s) {
            context.getSessionData().put("type", s);
            return new HowManyPrompt();
        }
    }

    private class HowManyPrompt extends NumericPrompt {
        public String getPromptText(ConversationContext context) {
            return "How many " + context.getSessionData().get("type") + "s would you like to summon?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() > 0 && input.intValue() <= 20;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "Input must be between 1 and 20.";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            context.getSessionData().put("count", number.intValue());
            return new ForWhomPrompt(plugin);
        }
    }

    private class ForWhomPrompt extends PlayerNamePrompt {
        public ForWhomPrompt(Plugin plugin) {
            super(plugin);
        }

        public String getPromptText(ConversationContext context) {
            return "Who should receive your " + context.getSessionData().get("type") + "s?";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Player player) {
            context.getSessionData().put("who", player);
            return new SummonedPrompt();
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, String invalidInput) {
            return invalidInput + " is not online!";
        }
    }

    private class SummonedPrompt extends MessagePrompt {
        public String getPromptText(ConversationContext context) {
            Player who = (Player)context.getSessionData().get("who");
            String what = (String)context.getSessionData().get("type");
            int count = (Integer)context.getSessionData().get("count");

            for (int i = 0; i < count; i++) {
                World world = who.getWorld();
                world.spawnCreature(who.getLocation(), CreatureType.fromName(what));
            }
            
            return "It is done!";
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }
    }
}
