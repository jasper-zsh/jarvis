# Change: WeChat-style message input bar

## Why
- Users expect a fast, familiar compose bar similar to WeChat for switching between text and hold-to-talk voice.
- Current input mixes many controls and lacks press-to-talk interaction, slowing frequent senders.

## What Changes
- Redesign the chat input bar to WeChat-like layout: mode toggle (text ↔ voice), hold-to-talk gesture area, emoji/attach shortcuts, and clear send affordance.
- Support press-and-hold to record with slide-to-cancel feedback; release-to-send behavior.
- Maintain existing photo/gallery entry points while keeping bar compact on small screens.

## Impact
- Affected specs: chat (input controls, voice handling)
- Affected code: chat compose UI, voice recording trigger/feedback, permission prompts, message send UX
