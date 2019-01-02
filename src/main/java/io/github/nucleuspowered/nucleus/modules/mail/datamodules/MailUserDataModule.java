/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.datamodules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.nucleusdata.MailMessage;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.modules.mail.data.MailData;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IUserDataModule;

import java.util.List;

public class MailUserDataModule implements IUserDataModule {

    @DataKey("mail")
    private List<MailData> mailDataList = Lists.newArrayList();

    public List<MailData> getMail() {
        return ImmutableList.copyOf(this.mailDataList);
    }

    public void addMail(MailData mailData) {
        if (this.mailDataList == null) {
            this.mailDataList = Lists.newArrayList();
        }

        this.mailDataList.add(mailData);
    }

    public boolean removeMail(MailMessage mailData) {
        return this.mailDataList.removeIf(x -> x.equals(mailData));
    }

    public boolean clearMail() {
        if (!this.mailDataList.isEmpty()) {
            this.mailDataList.clear();
            return true;
        } else {
            return false;
        }
    }
}
