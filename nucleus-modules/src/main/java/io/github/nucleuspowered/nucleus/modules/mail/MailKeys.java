/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail;

import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class MailKeys {

    public static final DataKey.ListKey<MailMessage, IUserDataObject> MAIL_DATA =
            DataKey.ofList(TypeTokens.MAIL_MESSAGE, IUserDataObject.class, "mailData");

}
