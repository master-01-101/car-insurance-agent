from typing import Literal
from pydantic import BaseModel, Field, ConfigDict, model_validator

class Principal(BaseModel):
    user_id: str
    salesperson_id: str
    role: Literal['user', 'salesperson'] = 'user'

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    conversation_id: str | None = None
    plate: str | None = None

class Intent(BaseModel):
    model_config = ConfigDict(extra='forbid')
    action: Literal['help', 'history', 'recommend', 'modify', 'confirm', 'quote',
                    'underwriting', 'status', 'payment', 'policy', 'handoff', 'cancel', 'clarify']
    amount: int | None = Field(default=None, gt=0, le=100000000)
    vehicle_damage: bool | None = None
    insurer: str | None = Field(default=None, max_length=50)
    question: str | None = Field(default=None, min_length=1, max_length=300)

    @model_validator(mode='after')
    def validate_action(self):
        if self.action == 'clarify' and not (self.question or '').strip():
            raise ValueError('追问必须提供具体问题')
        if self.action == 'modify' and all(value is None for value in
                                           (self.amount, self.vehicle_damage, self.insurer)):
            raise ValueError('修改必须提供至少一个明确字段')
        return self

class ConfirmRequest(BaseModel):
    token: str

class BatchRequest(BaseModel):
    items: list[ChatRequest] = Field(min_length=1, max_length=30)
