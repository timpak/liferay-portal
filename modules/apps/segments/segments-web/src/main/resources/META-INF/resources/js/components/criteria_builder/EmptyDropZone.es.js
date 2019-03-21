import getCN from 'classnames';
import PropTypes from 'prop-types';
import React, {Component} from 'react';
import ThemeContext from '../../ThemeContext.es';
import {DragTypes} from '../../utils/drag-types.es';
import {DropTarget as dropTarget} from 'react-dnd';

/**
 * Prevents items from being dropped from other contributors.
 * This method must be called `canDrop`.
 * @param {Object} props Component's current props.
 * @param {DropTargetMonitor} monitor
 * @returns {boolean} True if the target should accept the item.
 */
function canDrop(props, monitor) {
	const {propertyKey: destPropertyKey} = props;
	const {propertyKey: startPropertyKey} = monitor.getItem();

	return destPropertyKey === startPropertyKey;
}

/**
 * Implements the behavior of what will occur when an item is dropped.
 * Adds the criterion dropped.
 * This method must be called `drop`.
 * @param {Object} props Component's current props.
 * @param {DropTargetMonitor} monitor
 */
function drop(props, monitor) {
	const {criterion} = monitor.getItem();

	props.onCriterionAdd(0, criterion);
}

class EmptyDropZone extends Component {
	static contextType = ThemeContext;

	static propTypes = {
		canDrop: PropTypes.bool,
		connectDropTarget: PropTypes.func,
		hover: PropTypes.bool,
		onCriterionAdd: PropTypes.func.isRequired,
		propertyKey: PropTypes.string.isRequired
	};

	render() {
		const {
			canDrop,
			connectDropTarget,
			hover
		} = this.props;

		const {assetsPath} = this.context;

		const targetClasses = getCN(
			'empty-drop-zone-target',
			{
				'dnd-hover': canDrop && hover
			}
		);

		return (
			<div className="empty-drop-zone-root">
				{connectDropTarget(
					<div
						className={targetClasses}
					>
						<div className="empty-drop-zone-indicator" />

						<div className="empty-drop-zone-help-message">
							<div className="message-item">
								<img
									className="message-icon"
									src={`${assetsPath}/drag-and-drop.svg`}
								/>

								<span>
									{Liferay.Language.get('drag-and-drop-criterion-from-the-right-to-add-rules')}
								</span>
							</div>

							<div className="message-item">
								<img
									className="message-icon"
									src={`${assetsPath}/drag-over.svg`}
								/>

								<span>
									{Liferay.Language.get('drag-and-drop-over-an-existing-criteria-to-form-groups')}
								</span>
							</div>
						</div>
					</div>
				)}
			</div>
		);
	}
}

export default dropTarget(
	DragTypes.PROPERTY,
	{
		canDrop,
		drop
	},
	(connect, monitor) => ({
		canDrop: monitor.canDrop(),
		connectDropTarget: connect.dropTarget(),
		hover: monitor.isOver()
	})
)(EmptyDropZone);