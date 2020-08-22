/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.economy;

import io.github.nucleuspowered.nucleus.services.interfaces.IEconomyServiceProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import java.math.BigDecimal;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EconomyServiceProvider implements IEconomyServiceProvider {

    private final IMessageProviderService messageProviderService;

    @Inject
    public EconomyServiceProvider(IMessageProviderService messageProviderService) {
        this.messageProviderService = messageProviderService;
    }

    @Override
    public boolean serviceExists() {
        return Sponge.getServiceManager().provide(EconomyService.class).isPresent();
    }

    @Override public String getCurrencySymbol(double cost) {
        Optional<EconomyService> oes = Sponge.getServiceManager().provide(EconomyService.class);
        return oes.map(economyService -> economyService.getDefaultCurrency().format(BigDecimal.valueOf(cost)).toPlain())
                .orElseGet(() -> String.valueOf(cost));

    }

    @Override public boolean hasBalance(Player src, double balance) {
        Optional<EconomyService> oes = Sponge.getServiceManager().provide(EconomyService.class);
        if (oes.isPresent()) {
            // Check balance.
            EconomyService es = oes.get();
            Optional<UniqueAccount> ua = es.getOrCreateAccount(src.getUniqueId());
            return ua.isPresent() && ua.get().getBalance(es.getDefaultCurrency()).doubleValue() >= balance;
        }

        // No economy
        return true;
    }

    @Override public boolean withdrawFromPlayer(Player src, double cost) {
        return withdrawFromPlayer(src, cost, true);
    }

    @Override public boolean withdrawFromPlayer(Player src, double cost, boolean message) {
        Optional<EconomyService> oes = Sponge.getServiceManager().provide(EconomyService.class);
        if (oes.isPresent()) {
            // Check balance.
            EconomyService es = oes.get();
            Optional<UniqueAccount> a = es.getOrCreateAccount(src.getUniqueId());
            if (!a.isPresent()) {
                this.messageProviderService.sendMessageTo(src, "cost.noaccount");
                return false;
            }

            TransactionResult tr = a.get().withdraw(es.getDefaultCurrency(), BigDecimal.valueOf(cost), CauseStackHelper.createCause(src));
            if (tr.getResult() == ResultType.ACCOUNT_NO_FUNDS) {
                if (message) {
                    this.messageProviderService.sendMessageTo(src, "cost.nofunds", getCurrencySymbol(cost));
                }

                return false;
            } else if (tr.getResult() != ResultType.SUCCESS) {
                this.messageProviderService.sendMessageTo(src, "cost.error");
                return false;
            }

            if (message) {
                this.messageProviderService.sendMessageTo(src, "cost.complete", getCurrencySymbol(cost));
            }
        }

        return true;
    }

    @Override public boolean depositInPlayer(User src, double cost) {
        return depositInPlayer(src, cost, true);
    }

    @Override public boolean depositInPlayer(User src, double cost, boolean message) {
        Optional<EconomyService> oes = Sponge.getServiceManager().provide(EconomyService.class);
        if (oes.isPresent()) {
            // Check balance.
            EconomyService es = oes.get();
            Optional<UniqueAccount> a = es.getOrCreateAccount(src.getUniqueId());
            if (!a.isPresent()) {
                src.getPlayer().ifPresent(x ->
                        this.messageProviderService.sendMessageTo(x, "cost.noaccount"));
                return false;
            }

            TransactionResult tr = a.get().deposit(es.getDefaultCurrency(), BigDecimal.valueOf(cost), CauseStackHelper.createCause(src));
            if (tr.getResult() != ResultType.SUCCESS && src.isOnline()) {
                src.getPlayer().ifPresent(x ->
                        this.messageProviderService.sendMessageTo(x, "cost.error"));
                return false;
            }

            if (message && src.isOnline()) {
                src.getPlayer().ifPresent(x ->
                        this.messageProviderService.sendMessageTo(x, "cost.refund", getCurrencySymbol(cost)));
            }
        }

        return true;
    }

}
