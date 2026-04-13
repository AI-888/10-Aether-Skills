from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Optional

import yaml as yaml_lib
from fastapi import APIRouter, FastAPI
from fastapi.responses import HTMLResponse
from loguru import logger
from pydantic import BaseModel


class SkillSaveRequest(BaseModel):
    """Skill save request model."""

    dir: Optional[str] = None
    path: str
    content: str


class SkillDeleteRequest(BaseModel):
    """Skill delete request model."""

    dir: Optional[str] = None
    path: str


class SkillCreateRequest(BaseModel):
    """Skill create request model."""

    dir: Optional[str] = None
    path: str
    content: str


def _load_skill_page() -> str:
    """从 .10-Aether-Skills 目录加载 skill 管理页面。"""
    html_path = Path(__file__).resolve().parent / "skill.html"
    try:
        return html_path.read_text(encoding="utf-8")
    except FileNotFoundError:
        return "<html><body><h1>Template not found: skill.html</h1></body></html>"


def _resolve_skill_dirs() -> list[Path]:
    """从配置文件解析 Skill 目录列表（支持多目录）。"""
    config_path = Path(__file__).resolve().parent / "skill_config.yaml"

    candidates: list[str] = []
    if config_path.exists():
        try:
            data = yaml_lib.safe_load(config_path.read_text(encoding="utf-8"))
            if isinstance(data, dict):
                raw_dirs = data.get("skill_dirs", [])
                if isinstance(raw_dirs, list):
                    candidates = [str(x).strip() for x in raw_dirs if str(x).strip()]
        except Exception as e:
            logger.warning(f"[SKILL_API] Failed to read config file {config_path}: {e}")

    if not candidates:
        candidates = [str(Path.home() / ".nanobot" / "workspace" / "skills")]

    resolved: list[Path] = []
    seen: set[str] = set()
    for d in candidates:
        p = Path(d).expanduser()
        if not p.is_absolute():
            p = (config_path.parent / p).resolve()
        else:
            p = p.resolve()
        k = str(p)
        if k not in seen:
            seen.add(k)
            resolved.append(p)

    return resolved


def _pick_target_dir(dir_value: str | None, base_skill_dirs: list[Path]) -> tuple[Path | None, str | None]:
    """选择目标目录；若传入 dir_value 则必须命中已配置目录。"""
    if not base_skill_dirs:
        return None, "No skill directory configured"

    if dir_value:
        target = Path(dir_value).expanduser().resolve()
        for d in base_skill_dirs:
            if target == d:
                return d, None
        return None, "Invalid dir: not in configured skill directories"

    return base_skill_dirs[0], None


def create_router(skill_dirs: list[str | Path] | str | Path | None = None):
    """创建 Skill 管理 API 路由（独立项目，无 web 模块依赖）。"""
    router = APIRouter()
    if skill_dirs is None:
        base_skill_dirs = _resolve_skill_dirs()
    elif isinstance(skill_dirs, (str, Path)):
        base_skill_dirs = [Path(skill_dirs).expanduser().resolve()]
    else:
        base_skill_dirs = [Path(d).expanduser().resolve() for d in skill_dirs]

    @router.get("/skills")
    async def get_skills_page():
        """Serve the Skill management page."""
        return HTMLResponse(content=_load_skill_page())

    @router.get("/api/skills/list")
    async def list_skills():
        """List all skills from configured skill directories."""
        skills: dict[str, list[dict[str, Any]]] = {}

        for skill_dir in base_skill_dirs:
            if not skill_dir.exists():
                continue

            for root, dirs, files in os.walk(skill_dir):
                rel_root = Path(root).relative_to(skill_dir)
                category = str(rel_root) if str(rel_root) != "." else ""

                skill_files = [f for f in files if f.startswith("SKILL") and (f.endswith(".yaml") or f.endswith(".yml"))]

                if skill_files:
                    if category not in skills:
                        skills[category] = []

                    for filename in skill_files:
                        file_path = Path(root) / filename
                        skill_type = "workflow"
                        skill_name = ""
                        output_schema: dict[str, Any] = {}

                        try:
                            with open(file_path, "r", encoding="utf-8") as f:
                                doc = yaml_lib.safe_load(f)
                            if isinstance(doc, dict) and "skill" in doc:
                                skill_section = doc["skill"]
                                if isinstance(skill_section, dict):
                                    skill_type = skill_section.get("type", "workflow")
                                    skill_name = skill_section.get("name", "")
                                    raw_os = skill_section.get("output_schema", {})
                                    if isinstance(raw_os, dict):
                                        output_schema = raw_os
                        except Exception:
                            pass

                        skills[category].append(
                            {
                                "name": filename,
                                "skill_name": skill_name,
                                "path": str(Path(category) / filename) if category else filename,
                                "type": skill_type,
                                "output_schema": output_schema,
                                "dir": str(skill_dir),
                            }
                        )

        for cat in skills:
            skills[cat].sort(key=lambda x: (x["name"], x["dir"]))

        return {
            "skills": skills,
            "skill_dirs": [str(d) for d in base_skill_dirs],
            "existing_skill_dirs": [str(d) for d in base_skill_dirs if d.exists()],
        }

    @router.get("/api/skills/read")
    async def read_skill(path: str, dir: str | None = None):
        """Read skill file content."""
        skill_dir, err = _pick_target_dir(dir, base_skill_dirs)
        if err:
            return {"error": err}

        file_path = (skill_dir / path).resolve()

        try:
            file_path.relative_to(skill_dir)
        except ValueError:
            return {"error": "Access denied: path outside skill directory"}

        if not file_path.exists():
            return {"error": "File not found"}

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            return {"content": content, "dir": str(skill_dir)}
        except Exception as e:
            return {"error": str(e)}

    @router.post("/api/skills/save")
    async def save_skill(request: SkillSaveRequest):
        """Save skill file content."""
        skill_dir, err = _pick_target_dir(request.dir, base_skill_dirs)
        if err:
            return {"error": err}

        file_path = (skill_dir / request.path).resolve()

        try:
            file_path.relative_to(skill_dir)
        except ValueError:
            return {"error": "Access denied: path outside skill directory"}

        try:
            yaml_lib.safe_load(request.content)
            file_path.parent.mkdir(parents=True, exist_ok=True)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(request.content)

            logger.info(f"[SKILL_API] Saved skill file: {file_path}")
            return {"success": True, "message": "Saved successfully", "dir": str(skill_dir)}
        except yaml_lib.YAMLError as e:
            return {"error": f"Invalid YAML: {str(e)}"}
        except Exception as e:
            return {"error": str(e)}

    @router.post("/api/skills/create")
    async def create_skill(request: SkillCreateRequest):
        """Create a new skill file."""
        skill_dir, err = _pick_target_dir(request.dir, base_skill_dirs)
        if err:
            return {"error": err}

        if not skill_dir.exists():
            skill_dir.mkdir(parents=True, exist_ok=True)

        file_path = (skill_dir / request.path).resolve()

        try:
            file_path.relative_to(skill_dir)
        except ValueError:
            return {"error": "Access denied: path outside skill directory"}

        if file_path.exists():
            return {"error": "File already exists"}

        try:
            yaml_lib.safe_load(request.content)
            file_path.parent.mkdir(parents=True, exist_ok=True)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(request.content)

            logger.info(f"[SKILL_API] Created skill file: {file_path}")
            return {"success": True, "message": "Created successfully", "path": str(file_path), "dir": str(skill_dir)}
        except yaml_lib.YAMLError as e:
            return {"error": f"Invalid YAML: {str(e)}"}
        except Exception as e:
            return {"error": str(e)}

    @router.post("/api/skills/delete")
    async def delete_skill(request: SkillDeleteRequest):
        """Delete a skill file."""
        skill_dir, err = _pick_target_dir(request.dir, base_skill_dirs)
        if err:
            return {"error": err}

        file_path = (skill_dir / request.path).resolve()

        try:
            file_path.relative_to(skill_dir)
        except ValueError:
            return {"error": "Access denied: path outside skill directory"}

        if not file_path.exists():
            return {"error": "File not found"}

        try:
            file_path.unlink()
            logger.info(f"[SKILL_API] Deleted skill file: {file_path}")
            return {"success": True, "message": "Deleted successfully", "dir": str(skill_dir)}
        except Exception as e:
            return {"error": str(e)}

    return router


app = FastAPI(title="Aether Skills API", description="Skill 管理服务")
app.include_router(create_router())

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5500, access_log=False)
