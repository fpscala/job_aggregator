from __future__ import annotations

from datetime import datetime
from typing import Annotated

from pydantic import BaseModel, ConfigDict, StringConstraints


NonEmptyStr = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1)]


class Job(BaseModel):
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    title: NonEmptyStr
    company: str | None = None
    location: str | None = None
    salary: str | None = None
    description: NonEmptyStr
    source: NonEmptyStr
    url: NonEmptyStr
    posted_at: datetime
