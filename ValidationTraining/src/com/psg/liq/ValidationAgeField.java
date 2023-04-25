package com.psg.liq;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.misys.liq.Messages;
import com.misys.liq.bm.desktopcore.extender.domain.LiqObjectExtensionAttribute;
import com.misys.liq.bm.desktopcore.main.cdt.ObjectEvent;
import com.misys.liq.bm.desktopcore.main.cdt.loan.LoanInitialDrawdown;
import com.misys.liq.bm.main.goodies.LiqMNModelEnhancementRegistryEvent;
import com.misys.liq.bm.main.liqmaineventmanagementsubapp.LiqMNBusinessEventTrigger;
import com.misys.liq.inf.LiqEventAction;
import com.misys.liq.inf.LiqEventActionContext;
import com.misys.liq.infrastructure.exceptions.LiqError;
import com.sxsy.smtj.exceptions.ExceptionUtility;
import com.sxsy.smtj.utilities.StringUtility;

public class ValidationAgeField implements LiqEventAction {

	@Override
	public void execute(LiqMNModelEnhancementRegistryEvent event, LiqMNBusinessEventTrigger trigger,
			LiqEventActionContext context) {

		if (event == null)
			ExceptionUtility.throwException((Throwable) new LiqError(StringUtility
					.bindWith(Messages.liqNlsExternalizedMessage("Registry Event cannot be null."), new Object[0])));
		if (trigger == null)
			ExceptionUtility.throwException((Throwable) new LiqError(StringUtility
					.bindWith(Messages.liqNlsExternalizedMessage("Event trigger cannot be null."), new Object[0])));

		try {
			String addtionalField = "AGE";
			String addtionalFieldValue = "";

			if ("OTR".equals(trigger.getEventOwnerObjectTypeCode()) && "STA".equals(trigger.getEventCode())) {
				Object eventOwner = ((ObjectEvent) event.eventOwner()).evoOwner();

				if (eventOwner instanceof LoanInitialDrawdown) {
					LoanInitialDrawdown loan = (LoanInitialDrawdown) eventOwner;

					@SuppressWarnings("unchecked")
					List<LiqObjectExtensionAttribute> loanAddtionalList = loan.additionalFieldAttributes();

					for (LiqObjectExtensionAttribute loanAdditionalField : loanAddtionalList) {
						if (addtionalField.equals(loanAdditionalField.getName()))
							addtionalFieldValue = (String) loanAdditionalField.getTypedValue().getValue();
					}

					if (StringUtils.isBlank(addtionalFieldValue))
						ExceptionUtility.throwException((Throwable) new LiqError(StringUtility.bindWith(
								Messages.liqNlsExternalizedMessage(addtionalField + " cannot be null"),
								new Object[0])));

				}

			}
		} catch (Exception e) {
			ExceptionUtility.throwException((Throwable) new LiqError(StringUtility.bindWith(
					Messages.liqNlsExternalizedMessage("Exception error occurred. Message: " + e.getMessage()),
					new Object[0])));
		}
	}

}
