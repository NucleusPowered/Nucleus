/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail;

import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;

public final class MailKeys {

    public static final DataKey.ListKey<MailMessage, IUserDataObject> MAIL_DATA =
            DataKey.ofList(TypeTokens.MAIL_MESSAGE, IUserDataObject.class, "mailData");

}
