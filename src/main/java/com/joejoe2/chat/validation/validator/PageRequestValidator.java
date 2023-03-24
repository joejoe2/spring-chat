package com.joejoe2.chat.validation.validator;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.exception.ValidationError;

public class PageRequestValidator
    implements Validator<PageRequest, org.springframework.data.domain.PageRequest> {
  private static final PageRequestValidator instance = new PageRequestValidator();

  private PageRequestValidator() {}

  public static PageRequestValidator getInstance() {
    return instance;
  }

  @Override
  public org.springframework.data.domain.PageRequest validate(PageRequest data)
      throws ValidationError {
    if (data == null) throw new ValidationError("page request cannot be null !");
    if (data.getPage() == null) throw new ValidationError("page cannot be null !");
    if (data.getSize() == null) throw new ValidationError("page cannot be null !");
    if (data.getPage() < 0) throw new ValidationError("page must >=0 !");
    if (data.getSize() <= 0) throw new ValidationError("size must >=1 !");
    return org.springframework.data.domain.PageRequest.of(data.getPage(), data.getSize());
  }
}
