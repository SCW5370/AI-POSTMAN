from abc import ABC, abstractmethod
from typing import List, Dict


class BaseFetcher(ABC):
    @abstractmethod
    def fetch(self, source: Dict) -> List[Dict]:
        raise NotImplementedError
